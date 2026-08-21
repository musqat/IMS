package com.ims.item.repository;

import com.ims.item.entity.Bom;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BomRepository extends JpaRepository<Bom, Long> {

    /**
     * 특정 품목이 parent인 BOM 목록 조회
     * - BomResponse가 parent·child의 코드·이름을 읽으므로 함께 로딩한다
     */
    @EntityGraph(attributePaths = {"parent", "child"})
    List<Bom> findAllByParentId(Long parentId);

    /** 특정 유저 소유 BOM 전체 조회 */
    @EntityGraph(attributePaths = {"parent", "child"})
    List<Bom> findAllByParentOwnerId(Long ownerId);

    /** parent + child 쌍 중복 여부 확인 */
    boolean existsByParentIdAndChildId(Long parentId, Long childId);

    /** BOM 참조 여부 확인 — item이 parent 또는 child로 사용 중인지 검사 */
    boolean existsByParentId(Long parentId);

    boolean existsByChildId(Long childId);
}
