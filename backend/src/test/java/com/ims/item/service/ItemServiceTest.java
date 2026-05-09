package com.ims.item.service;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.global.support.DomainValidator;
import com.ims.item.dto.request.ItemCreateRequest;
import com.ims.item.dto.response.ItemResponse;
import com.ims.item.entity.Item;
import com.ims.item.entity.ItemType;
import com.ims.item.repository.BomRepository;
import com.ims.item.repository.ItemRepository;
import com.ims.user.entity.User;
import com.ims.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @InjectMocks
    private ItemService itemService;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BomRepository bomRepository;

    @Mock
    private DomainValidator domainValidator;

    private User owner;
    private Item item;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .email("test@test.com")
                .password("encodedPassword")
                .companyName("테스트회사")
                .companyCode("1000000001")
                .build();

        item = Item.builder()
                .id(1L)
                .owner(owner)
                .itemCode("ITEM-001")
                .name("테스트 품목")
                .type(ItemType.PRODUCT)
                .build();
    }

    @Test
    @DisplayName("품목 생성 성공")
    void createItem_success() {
        // given
        ItemCreateRequest request = new ItemCreateRequest("ITEM-001", "테스트 품목", ItemType.PRODUCT, null);
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(itemRepository.existsByOwnerIdAndItemCode(1L, "ITEM-001")).willReturn(false);
        given(itemRepository.save(any(Item.class))).willReturn(item);

        // when
        ItemResponse response = itemService.createItem(1L, request);

        // then
        assertThat(response.itemCode()).isEqualTo("ITEM-001");
        assertThat(response.name()).isEqualTo("테스트 품목");
        assertThat(response.type()).isEqualTo(ItemType.PRODUCT);
        then(itemRepository).should().save(any(Item.class));
    }

    @Test
    @DisplayName("품목 생성 실패 - 중복 itemCode")
    void createItem_duplicateCode() {
        ItemCreateRequest request = new ItemCreateRequest("ITEM-001", "테스트 품목", ItemType.PRODUCT, null);
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(itemRepository.existsByOwnerIdAndItemCode(1L, "ITEM-001")).willReturn(true);

        assertThatThrownBy(() -> itemService.createItem(1L, request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_ITEM_CODE);
    }

    @Test
    @DisplayName("품목 생성 실패 - 존재하지 않는 User")
    void createItem_userNotFound() {
        ItemCreateRequest request = new ItemCreateRequest("ITEM-001", "테스트 품목", ItemType.PRODUCT, null);
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.createItem(1L, request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("내 회사 품목 전체 조회 성공")
    void getItems_success() {
        given(itemRepository.findAllByOwnerId(1L)).willReturn(List.of(item));

        List<ItemResponse> result = itemService.getItems(1L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().itemCode()).isEqualTo("ITEM-001");
    }

    @Test
    @DisplayName("품목 단건 조회 성공")
    void getItem_success() {
        given(domainValidator.getOwnedItem(1L, 1L)).willReturn(item);

        ItemResponse result = itemService.getItem(1L, 1L);

        assertThat(result.itemCode()).isEqualTo("ITEM-001");
        assertThat(result.name()).isEqualTo("테스트 품목");
    }

    @Test
    @DisplayName("품목 단건 조회 실패 - 다른 소유자")
    void getItem_notOwner() {
        given(domainValidator.getOwnedItem(2L, 1L)).willThrow(new ImsException(ErrorCode.ITEM_NOT_OWNED));

        assertThatThrownBy(() -> itemService.getItem(2L, 1L))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ITEM_NOT_OWNED);
    }

    @Test
    @DisplayName("품목 삭제 성공")
    void deleteItem_success() {
        given(domainValidator.getOwnedItem(1L, 1L)).willReturn(item);
        given(bomRepository.existsByParentId(1L)).willReturn(false);
        given(bomRepository.existsByChildId(1L)).willReturn(false);

        itemService.deleteItem(1L, 1L);

        then(itemRepository).should().delete(item);
    }

    @Test
    @DisplayName("품목 삭제 실패 - 소유자 아님")
    void deleteItem_notOwner() {
        given(domainValidator.getOwnedItem(2L, 1L)).willThrow(new ImsException(ErrorCode.ITEM_NOT_OWNED));

        assertThatThrownBy(() -> itemService.deleteItem(2L, 1L))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ITEM_NOT_OWNED);
    }

    @Test
    @DisplayName("품목 삭제 실패 - BOM에 등록된 품목")
    void deleteItem_bomInUse() {
        given(domainValidator.getOwnedItem(1L, 1L)).willReturn(item);
        given(bomRepository.existsByParentId(1L)).willReturn(true);

        assertThatThrownBy(() -> itemService.deleteItem(1L, 1L))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ITEM_IN_USE_BY_BOM);
    }
}
