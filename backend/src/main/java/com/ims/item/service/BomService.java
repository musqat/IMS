package com.ims.item.service;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.global.support.DomainValidator;
import com.ims.item.dto.request.BomCreateRequest;
import com.ims.item.dto.response.BomResponse;
import com.ims.item.entity.Bom;
import com.ims.item.entity.Item;
import com.ims.item.repository.BomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BomService {

    private final BomRepository bomRepository;
    private final DomainValidator domainValidator;

    /**
     * BOM 등록
     * - 자기참조, 소유자, 중복, 순환참조 검증
     * - BOM 저장 후 응답 반환
     */
    @Transactional
    public BomResponse addBom(Long userId, Long parentItemId, BomCreateRequest request) {
        Long childItemId = request.childItemId();

        if (parentItemId.equals(childItemId)) {
            throw new ImsException(ErrorCode.BOM_SELF_REFERENCE);
        }

        Item parentItem = domainValidator.getOwnedItem(userId, parentItemId);
        Item childItem = domainValidator.getOwnedItem(userId, childItemId);

        if (bomRepository.existsByParentIdAndChildId(parentItemId, childItemId)) {
            throw new ImsException(ErrorCode.DUPLICATE_BOM);
        }

        // 해당 유저의 BOM 전체를 한 번만 로드 → 인메모리 DFS
        Map<Long, List<Long>> adjacency = buildAdjacency(userId);
        if (hasCycle(childItemId, parentItemId, new HashSet<>(), adjacency)) {
            throw new ImsException(ErrorCode.BOM_CIRCULAR_REFERENCE);
        }

        Bom bom = Bom.builder()
                .parent(parentItem)
                .child(childItem)
                .quantity(request.quantity())
                .build();

        return BomResponse.from(bomRepository.save(bom));
    }

    /**
     * 특정 품목의 직접 하위 BOM 목록 조회
     * - 소유자 검증 후 해당 품목의 직접 자식 BOM 반환
     */
    public List<BomResponse> getBoms(Long userId, Long parentItemId) {
        domainValidator.getOwnedItem(userId, parentItemId);
        return bomRepository.findAllByParentId(parentItemId)
                .stream().map(BomResponse::from).toList();
    }

    /**
     * BOM 삭제
     * - BOM 조회 후 parent Item 소유자 검증
     */
    @Transactional
    public void deleteBom(Long userId, Long bomId) {
        Bom bom = bomRepository.findById(bomId)
                .orElseThrow(() -> new ImsException(ErrorCode.BOM_NOT_FOUND));
        if (!bom.getParent().getOwner().getId().equals(userId)) {
            throw new ImsException(ErrorCode.ITEM_NOT_OWNED);
        }
        bomRepository.delete(bom);
    }


    /**
     * BOM 트리 전체 탐색 → 완성품 1개 기준 필요 수량 플랫 맵 반환 (long 반환으로 overflow 방지)
     * - 예: A → B(2) → C(3) 이면 { B: 2, C: 6 }
     * - DAG(공유 부품): 동일 부품은 각 경로 수량의 합산
     *   예: A→B(2), A→C(3), B→D(4), C→D(5) → D: 2*4 + 3*5 = 23
     * - subtreeCache로 이미 탐색한 노드의 서브트리를 재활용, 지수적 중복 탐색 방지
     * - ownerId를 받아 한 번의 DB 조회로 전체 BOM 로드
     */
    @Transactional(readOnly = true, noRollbackFor = Exception.class)
    public Map<Long, Long> getFullBomTree(Long parentItemId, Long ownerId) {
        Map<Long, List<BomEdge>> adjacency = buildQuantityAdjacency(ownerId);
        Map<Long, Long> result = new HashMap<>();
        Map<Long, Map<Long, Long>> subtreeCache = new HashMap<>();
        collectBomTree(parentItemId, 1L, result, 0, subtreeCache, adjacency);
        return result;
    }

    /**
     * 여러 완성품에 대한 BOM 트리를 한 번의 DB 조회로 일괄 계산
     * - DB 조회를 1회만 수행하여 N개 품목의 트리를 계산
     * - getShortageAnalysis 등 완성품 전체를 분석할 때 사용
     */
    @Transactional(readOnly = true, noRollbackFor = Exception.class)
    public Map<Long, Map<Long, Long>> getFullBomTrees(List<Long> parentItemIds, Long ownerId) {
        Map<Long, List<BomEdge>> adjacency = buildQuantityAdjacency(ownerId); // DB 1회
        Map<Long, Map<Long, Long>> subtreeCache = new HashMap<>();
        Map<Long, Map<Long, Long>> result = new HashMap<>();
        for (Long parentItemId : parentItemIds) {
            Map<Long, Long> tree = new HashMap<>();
            collectBomTree(parentItemId, 1L, tree, 0, subtreeCache, adjacency);
            result.put(parentItemId, tree);
        }
        return result;
    }

    // ======================== 헬퍼 메소드 ===================== //

    /**
     * getFullBomTree DFS 재귀 헬퍼
     */
    private void collectBomTree(Long itemId, long multiplier, Map<Long, Long> result,
                                int depth, Map<Long, Map<Long, Long>> subtreeCache,
                                Map<Long, List<BomEdge>> adjacency) {
        if (depth >= MAX_BOM_DEPTH) {
            throw new ImsException(ErrorCode.BOM_DEPTH_EXCEEDED);
        }

        // 이미 탐색한 서브트리는 스케일링만 하고 재귀 생략
        if (subtreeCache.containsKey(itemId)) {
            subtreeCache.get(itemId).forEach((partId, unitQty) ->
                    result.merge(partId, unitQty * multiplier, Long::sum));
            return;
        }

        List<BomEdge> children = adjacency.getOrDefault(itemId, List.of());
        Map<Long, Long> unitSubtree = new HashMap<>();

        for (BomEdge edge : children) {
            Long childId = edge.childId();
            long unitQty = edge.quantity();

            unitSubtree.merge(childId, unitQty, Long::sum);
            result.merge(childId, unitQty * multiplier, Long::sum);

            collectBomTree(childId, unitQty * multiplier, result, depth + 1, subtreeCache, adjacency);

            // 자식의 서브트리를 현재 노드 서브트리에 흡수 (단위 수량 기준)
            if (subtreeCache.containsKey(childId)) {
                subtreeCache.get(childId).forEach((partId, childUnitQty) ->
                        unitSubtree.merge(partId, childUnitQty * unitQty, Long::sum));
            }
        }

        subtreeCache.put(itemId, unitSubtree);
    }


    /**
     * 유저의 BOM 전체를 인접 리스트(parentId → childId 목록)로 변환
     */
    private Map<Long, List<Long>> buildAdjacency(Long userId) {
        return bomRepository.findAllByParentOwnerId(userId).stream()
                .collect(Collectors.groupingBy(
                        b -> b.getParent().getId(),
                        Collectors.mapping(b -> b.getChild().getId(), Collectors.toList())
                ));
    }

    /**
     * 유저의 BOM 전체를 수량 포함 인접 리스트로 변환 (getFullBomTree)
     * - DB 한 번 호출로 전체 BOM 로드
     */
    private Map<Long, List<BomEdge>> buildQuantityAdjacency(Long ownerId) {
        return bomRepository.findAllByParentOwnerId(ownerId).stream()
                .collect(Collectors.groupingBy(
                        b -> b.getParent().getId(),
                        Collectors.mapping(b -> new BomEdge(b.getChild().getId(), b.getQuantity()),
                                Collectors.toList())
                ));
    }

    private record BomEdge(Long childId, int quantity) {
    }

    /**
     * 순환 참조 탐색
     * - adjacency: 사전 로드된 인접 리스트
     * - targetId 하위 트리에 searchId가 있으면 true
     */
    private boolean hasCycle(Long targetId, Long searchId, Set<Long> visited, Map<Long, List<Long>> adjacency) {
        if (visited.contains(targetId)) return false;
        visited.add(targetId);

        for (Long childId : adjacency.getOrDefault(targetId, List.of())) {
            if (childId.equals(searchId)) return true;
            if (hasCycle(childId, searchId, visited, adjacency)) return true;
        }

        return false;
    }

    private static final int MAX_BOM_DEPTH = 20;

}
