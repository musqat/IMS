package com.ims.item.repository;

import com.ims.item.entity.Item;
import com.ims.item.entity.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    /** 회사(owner) 소유 품목 전체 조회 */
    List<Item> findAllByOwnerId(Long ownerId);

    /** item_code + owner 조합으로 단건 조회  */
    Optional<Item> findByOwnerIdAndItemCode(Long ownerId, String itemCode);

    /** item_code 중복 여부 확인 */
    boolean existsByOwnerIdAndItemCode(Long ownerId, String itemCode);

    /** 타입별 품목 조회 */
    List<Item> findAllByOwnerIdAndType(Long ownerId, ItemType type);
}
