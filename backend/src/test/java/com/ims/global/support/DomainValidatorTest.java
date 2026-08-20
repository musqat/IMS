package com.ims.global.support;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.item.entity.Item;
import com.ims.item.entity.ItemType;
import com.ims.item.repository.ItemRepository;
import com.ims.user.entity.User;
import com.ims.warehouse.entity.Warehouse;
import com.ims.warehouse.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * DomainValidator 단위 테스트
 * - 6개 서비스에서 21회 호출되는 소유권 검증 헬퍼
 */
@ExtendWith(MockitoExtension.class)
class DomainValidatorTest {

    @InjectMocks
    private DomainValidator domainValidator;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private ItemRepository itemRepository;

    private User owner;
    private User other;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .email("owner@ims.dev")
                .password("encoded")
                .companyName("소유자회사")
                .companyCode("1000000001")
                .build();

        other = User.builder()
                .id(2L)
                .email("other@ims.dev")
                .password("encoded")
                .companyName("타사")
                .companyCode("1000000002")
                .build();
    }

    private Warehouse warehouseOf(User warehouseOwner) {
        return Warehouse.builder()
                .id(1L)
                .owner(warehouseOwner)
                .name("서울 조립창고")
                .location("서울특별시 금천구")
                .build();
    }

    private Item itemOf(User itemOwner) {
        return Item.builder()
                .id(1L)
                .owner(itemOwner)
                .itemCode("P001")
                .name("PCB기판")
                .type(ItemType.PART)
                .build();
    }

    // ===================== getOwnedWarehouse =====================

    @Test
    @DisplayName("창고 소유자가 조회하면 창고를 반환한다")
    void getOwnedWarehouse_owner_returnsWarehouse() {
        // given
        Warehouse warehouse = warehouseOf(owner);
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));

        // when
        Warehouse result = domainValidator.getOwnedWarehouse(owner.getId(), 1L);

        // then
        assertThat(result).isSameAs(warehouse);
    }

    @Test
    @DisplayName("존재하지 않는 창고면 WAREHOUSE_NOT_FOUND")
    void getOwnedWarehouse_notFound_throws() {
        // given
        given(warehouseRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> domainValidator.getOwnedWarehouse(owner.getId(), 999L))
                .isInstanceOf(ImsException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WAREHOUSE_NOT_FOUND);
    }

    @Test
    @DisplayName("소유자가 아니면 WAREHOUSE_NOT_OWNED")
    void getOwnedWarehouse_notOwner_throws() {
        // given — owner의 창고를 other가 조회 시도
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouseOf(owner)));

        // when & then
        assertThatThrownBy(() -> domainValidator.getOwnedWarehouse(other.getId(), 1L))
                .isInstanceOf(ImsException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WAREHOUSE_NOT_OWNED);
    }

    // ===================== getOwnedItem =====================

    @Test
    @DisplayName("품목 소유자가 조회하면 품목을 반환한다")
    void getOwnedItem_owner_returnsItem() {
        // given
        Item item = itemOf(owner);
        given(itemRepository.findById(1L)).willReturn(Optional.of(item));

        // when
        Item result = domainValidator.getOwnedItem(owner.getId(), 1L);

        // then
        assertThat(result).isSameAs(item);
    }

    @Test
    @DisplayName("존재하지 않는 품목이면 ITEM_NOT_FOUND")
    void getOwnedItem_notFound_throws() {
        // given
        given(itemRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> domainValidator.getOwnedItem(owner.getId(), 999L))
                .isInstanceOf(ImsException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ITEM_NOT_FOUND);
    }

    @Test
    @DisplayName("소유자가 아니면 ITEM_NOT_OWNED")
    void getOwnedItem_notOwner_throws() {
        // given — owner의 품목을 other가 조회 시도
        given(itemRepository.findById(1L)).willReturn(Optional.of(itemOf(owner)));

        // when & then
        assertThatThrownBy(() -> domainValidator.getOwnedItem(other.getId(), 1L))
                .isInstanceOf(ImsException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ITEM_NOT_OWNED);
    }
}
