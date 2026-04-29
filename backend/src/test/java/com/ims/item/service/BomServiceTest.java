package com.ims.item.service;

import com.ims.global.exception.ImsException;
import com.ims.item.dto.request.BomCreateRequest;
import com.ims.item.dto.response.BomResponse;
import com.ims.item.entity.Bom;
import com.ims.item.entity.Item;
import com.ims.item.entity.ItemType;
import com.ims.item.repository.BomRepository;
import com.ims.item.repository.ItemRepository;
import com.ims.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class BomServiceTest {

    @InjectMocks
    private BomService bomService;

    @Mock
    private BomRepository bomRepository;

    @Mock
    private ItemRepository itemRepository;

    private User owner;
    private Item itemA;  // FINISHED
    private Item itemB;  // SEMI
    private Item itemC;  // PART

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .email("test@test.com")
                .password("encoded")
                .companyName("TestCo")
                .companyCode("TC001")
                .build();

        itemA = Item.builder().id(1L).owner(owner).itemCode("A").name("완성품A").type(ItemType.FINISHED).build();
        itemB = Item.builder().id(2L).owner(owner).itemCode("B").name("반제품B").type(ItemType.SEMI).build();
        itemC = Item.builder().id(3L).owner(owner).itemCode("C").name("부품C").type(ItemType.PART).build();
    }

    @Test
    @DisplayName("BOM 등록 성공 - A(FINISHED) → B(SEMI), quantity=2")
    void addBom_success() {
        // given
        BomCreateRequest request = new BomCreateRequest(itemB.getId(), 2);
        Bom savedBom = Bom.builder().id(10L).parent(itemA).child(itemB).quantity(2).build();

        given(itemRepository.findById(itemA.getId())).willReturn(Optional.of(itemA));
        given(itemRepository.findById(itemB.getId())).willReturn(Optional.of(itemB));
        given(bomRepository.existsByParentIdAndChildId(itemA.getId(), itemB.getId())).willReturn(false);
        given(bomRepository.findAllByParentId(itemB.getId())).willReturn(List.of());
        given(bomRepository.save(any(Bom.class))).willReturn(savedBom);

        // when
        BomResponse response = bomService.addBom(owner.getId(), itemA.getId(), request);

        // then
        assertThat(response.parentItemId()).isEqualTo(itemA.getId());
        assertThat(response.childItemId()).isEqualTo(itemB.getId());
        assertThat(response.quantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("BOM 등록 실패 - 자기 자신 참조 (parent == child)")
    void addBom_selfReference() {
        // given
        BomCreateRequest request = new BomCreateRequest(itemA.getId(), 2);
        // when & then
        assertThatThrownBy(() -> bomService.addBom(owner.getId(), itemA.getId(), request))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("BOM 등록 실패 - 중복 BOM (A → B 이미 존재)")
    void addBom_duplicate() {
        // given
        BomCreateRequest request = new BomCreateRequest(itemB.getId(), 2);

        given(itemRepository.findById(itemA.getId())).willReturn(Optional.of(itemA));
        given(itemRepository.findById(itemB.getId())).willReturn(Optional.of(itemB));
        given(bomRepository.existsByParentIdAndChildId(itemA.getId(), itemB.getId())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> bomService.addBom(owner.getId(), itemA.getId(), request))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("BOM 등록 실패 - 순환 참조 감지 (A→B→C→A 시도)")
    void addBom_circularReference() {
        // given
        BomCreateRequest request = new BomCreateRequest(itemA.getId(), 2);

        given(itemRepository.findById(itemC.getId())).willReturn(Optional.of(itemC));
        given(itemRepository.findById(itemA.getId())).willReturn(Optional.of(itemA));
        given(bomRepository.existsByParentIdAndChildId(itemC.getId(), itemA.getId())).willReturn(false);

        given(bomRepository.findAllByParentId(itemA.getId()))
                .willReturn(List.of(Bom.builder().parent(itemA).child(itemB).quantity(1).build()));
        given(bomRepository.findAllByParentId(itemB.getId()))
                .willReturn(List.of(Bom.builder().parent(itemB).child(itemC).quantity(1).build()));

        // when & then
        assertThatThrownBy(() -> bomService.addBom(owner.getId(), itemC.getId(), request))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("BOM 등록 실패 - parent Item 소유자 아님")
    void addBom_notOwner() {
        // given
        BomCreateRequest request = new BomCreateRequest(itemB.getId(), 2);

        given(itemRepository.findById(itemA.getId())).willReturn(Optional.of(itemA));

        // when & then
        assertThatThrownBy(() -> bomService.addBom(222L, itemA.getId(), request))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("하위 BOM 목록 조회 성공")
    void getBoms_success() {
        // given
        Bom bom = Bom.builder().id(10L).parent(itemA).child(itemB).quantity(2).build();
        given(itemRepository.findById(itemA.getId())).willReturn(Optional.of(itemA));
        given(bomRepository.findAllByParentId(itemA.getId())).willReturn(List.of(bom));

        // when
        List<BomResponse> result = bomService.getBoms(owner.getId(), itemA.getId());

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().childItemId()).isEqualTo(itemB.getId());
    }

    @Test
    @DisplayName("하위 BOM 목록 조회 실패 - 소유자 아님")
    void getBoms_notOwner() {
        // given
        given(itemRepository.findById(itemA.getId())).willReturn(Optional.of(itemA));

        // when & then
        assertThatThrownBy(() -> bomService.getBoms(222L, itemA.getId()))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("BOM 삭제 성공")
    void deleteBom_success() {
        // given
        Bom bom = Bom.builder().id(10L).parent(itemA).child(itemB).quantity(2).build();
        given(bomRepository.findById(10L)).willReturn(Optional.of(bom));

        // when
        bomService.deleteBom(owner.getId(), 10L);

        // then
        then(bomRepository).should().delete(bom);
    }

    @Test
    @DisplayName("BOM 삭제 실패 - 소유자 아님")
    void deleteBom_notOwner() {
        // given
        Bom bom = Bom.builder().id(10L).parent(itemA).child(itemB).quantity(2).build();
        given(bomRepository.findById(10L)).willReturn(Optional.of(bom));

        // when & then
        assertThatThrownBy(() -> bomService.deleteBom(222L, 10L))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("getFullBomTree - 단일 레벨: A → B(qty=2)")
    void getFullBomTree_singleLevel() {
        // given
        Bom bomAB = Bom.builder().id(10L).parent(itemA).child(itemB).quantity(2).build();
        given(bomRepository.findAllByParentId(itemA.getId())).willReturn(List.of(bomAB));
        given(bomRepository.findAllByParentId(itemB.getId())).willReturn(List.of());

        // when
        Map<Long, Integer> result = bomService.getFullBomTree(itemA.getId());

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(itemB.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("getFullBomTree - 다단계: A → B(2) → C(3), C 누적 수량 = 6")
    void getFullBomTree_multiLevel() {
        // given
        Bom bomAB = Bom.builder().id(10L).parent(itemA).child(itemB).quantity(2).build();
        Bom bomBC = Bom.builder().id(11L).parent(itemB).child(itemC).quantity(3).build();
        given(bomRepository.findAllByParentId(itemA.getId())).willReturn(List.of(bomAB));
        given(bomRepository.findAllByParentId(itemB.getId())).willReturn(List.of(bomBC));
        given(bomRepository.findAllByParentId(itemC.getId())).willReturn(List.of());

        // when
        Map<Long, Integer> result = bomService.getFullBomTree(itemA.getId());

        // then
        assertThat(result).containsEntry(itemB.getId(), 2);
        assertThat(result).containsEntry(itemC.getId(), 6);
    }
}
