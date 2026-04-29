package com.ims.item.service;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.item.dto.request.BomCreateRequest;
import com.ims.item.dto.response.BomResponse;
import com.ims.item.entity.Bom;
import com.ims.item.entity.Item;
import com.ims.item.repository.BomRepository;
import com.ims.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BomService {

    private final BomRepository bomRepository;
    private final ItemRepository itemRepository;

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

        Item parentItem = itemRepository.findById(parentItemId)
                .orElseThrow(() -> new ImsException(ErrorCode.ITEM_NOT_FOUND));
        if (!parentItem.getOwner().getId().equals(userId)) {
            throw new ImsException(ErrorCode.ITEM_NOT_OWNED);
        }

        Item childItem = itemRepository.findById(childItemId)
                .orElseThrow(() -> new ImsException(ErrorCode.ITEM_NOT_FOUND));
        if (!childItem.getOwner().getId().equals(userId)) {
            throw new ImsException(ErrorCode.ITEM_NOT_OWNED);
        }

        if (bomRepository.existsByParentIdAndChildId(parentItemId, childItemId)) {
            throw new ImsException(ErrorCode.DUPLICATE_BOM);
        }

        if (hasCycle(childItemId, parentItemId, new HashSet<>())) {
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
     * - 소유자 검증 → ITEM_NOT_OWNED
     */
    public List<BomResponse> getBoms(Long userId, Long parentItemId) {
        Item parentItem = itemRepository.findById(parentItemId)
                .orElseThrow(() -> new ImsException(ErrorCode.ITEM_NOT_FOUND));
        if (!parentItem.getOwner().getId().equals(userId)) {
            throw new ImsException(ErrorCode.ITEM_NOT_OWNED);
        }

        return bomRepository.findAllByParentId(parentItemId)
                .stream().map(BomResponse::from).toList();
    }

    /**
     * BOM 삭제
     * - BOM 조회 후 parent Item 소유자 검증 → ITEM_NOT_OWNED
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
     * 순환 참조 탐색 (DFS)
     * - targetId 하위 트리에 searchId가 있으면 true
     */
    private boolean hasCycle(Long targetId, Long searchId, Set<Long> visited) {
        if (visited.contains(targetId)) return false;
        visited.add(targetId);

        List<Bom> children = bomRepository.findAllByParentId(targetId);

        for (Bom bom : children) {
            Long childItemId = bom.getChild().getId();
            if (childItemId.equals(searchId)) return true;
            if (hasCycle(childItemId, searchId, visited)) return true;  // 재귀
        }

        return false;
    }

    /**
     * BOM 트리 전체 탐색 → 완성품 1개 기준 필요 수량 플랫 맵 반환
     * - 예: A → B(2) → C(3) 이면 { B: 2, C: 6 }
     */
    public Map<Long, Integer> getFullBomTree(Long parentItemId) {
        Map<Long, Integer> result = new HashMap<>();
        collectBomTree(parentItemId, 1, result);
        return result;
    }

    /** getFullBomTree DFS 재귀 헬퍼 */
    private void collectBomTree(Long itemId, int multiplier, Map<Long, Integer> result) {
        List<Bom> boms = bomRepository.findAllByParentId(itemId);
        for (Bom bom : boms) {
            Long childId = bom.getChild().getId();
            int qty = bom.getQuantity() * multiplier;
            result.merge(childId, qty, Integer::sum);
            collectBomTree(childId, qty, result);
        }
    }
}
