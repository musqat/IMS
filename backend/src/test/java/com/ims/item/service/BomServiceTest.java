package com.ims.item.service;

import com.ims.global.exception.ImsException;
import com.ims.global.exception.ErrorCode;
import com.ims.global.support.DomainValidator;
import com.ims.item.dto.request.BomCreateRequest;
import com.ims.item.dto.response.BomResponse;
import com.ims.item.entity.Bom;
import com.ims.item.entity.Item;
import com.ims.item.entity.ItemType;
import com.ims.item.repository.BomRepository;
import com.ims.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
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
    private DomainValidator domainValidator;

    private User owner;
    private Item itemA;  // PRODUCT
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

        itemA = Item.builder().id(1L).owner(owner).itemCode("A").name("완성품A").type(ItemType.PRODUCT).build();
        itemB = Item.builder().id(2L).owner(owner).itemCode("B").name("반제품B").type(ItemType.SEMI).build();
        itemC = Item.builder().id(3L).owner(owner).itemCode("C").name("부품C").type(ItemType.PART).build();
    }

    @Test
    @DisplayName("BOM 등록 성공 - A(PRODUCT) → B(SEMI), quantity=2")
    void addBom_success() {
        // given
        BomCreateRequest request = new BomCreateRequest(itemB.getId(), 2);
        Bom savedBom = Bom.builder().id(10L).parent(itemA).child(itemB).quantity(2).build();

        given(domainValidator.getOwnedItem(owner.getId(), itemA.getId())).willReturn(itemA);
        given(domainValidator.getOwnedItem(owner.getId(), itemB.getId())).willReturn(itemB);
        given(bomRepository.existsByParentIdAndChildId(itemA.getId(), itemB.getId())).willReturn(false);
        given(bomRepository.findAllByParentOwnerId(owner.getId())).willReturn(List.of());
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
        // when & then: 자기참조 검증은 domainValidator 호출 전에 발생
        assertThatThrownBy(() -> bomService.addBom(owner.getId(), itemA.getId(), request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOM_SELF_REFERENCE);
    }

    @Test
    @DisplayName("BOM 등록 실패 - 중복 BOM (A → B 이미 존재)")
    void addBom_duplicate() {
        // given
        BomCreateRequest request = new BomCreateRequest(itemB.getId(), 2);

        given(domainValidator.getOwnedItem(owner.getId(), itemA.getId())).willReturn(itemA);
        given(domainValidator.getOwnedItem(owner.getId(), itemB.getId())).willReturn(itemB);
        given(bomRepository.existsByParentIdAndChildId(itemA.getId(), itemB.getId())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> bomService.addBom(owner.getId(), itemA.getId(), request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_BOM);
    }

    @Test
    @DisplayName("BOM 등록 실패 - 순환 참조 감지 (A→B→C→A 시도)")
    void addBom_circularReference() {
        // given: C → A 를 추가하려는데 A→B→C 가 이미 있음
        // findAllByParentOwnerId 로 한 번에 전체 BOM 로드 → 인메모리 DFS
        BomCreateRequest request = new BomCreateRequest(itemA.getId(), 2);

        given(domainValidator.getOwnedItem(owner.getId(), itemC.getId())).willReturn(itemC);
        given(domainValidator.getOwnedItem(owner.getId(), itemA.getId())).willReturn(itemA);
        given(bomRepository.existsByParentIdAndChildId(itemC.getId(), itemA.getId())).willReturn(false);
        given(bomRepository.findAllByParentOwnerId(owner.getId())).willReturn(List.of(
                Bom.builder().parent(itemA).child(itemB).quantity(1).build(),
                Bom.builder().parent(itemB).child(itemC).quantity(1).build()
        ));

        // when & then
        assertThatThrownBy(() -> bomService.addBom(owner.getId(), itemC.getId(), request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOM_CIRCULAR_REFERENCE);
    }

    @Test
    @DisplayName("BOM 등록 실패 - parent Item 소유자 아님")
    void addBom_notOwner() {
        // given
        BomCreateRequest request = new BomCreateRequest(itemB.getId(), 2);
        given(domainValidator.getOwnedItem(222L, itemA.getId())).willThrow(new ImsException(ErrorCode.ITEM_NOT_OWNED));

        // when & then
        assertThatThrownBy(() -> bomService.addBom(222L, itemA.getId(), request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ITEM_NOT_OWNED);
    }

    @Test
    @DisplayName("하위 BOM 목록 조회 성공")
    void getBoms_success() {
        // given
        Bom bom = Bom.builder().id(10L).parent(itemA).child(itemB).quantity(2).build();
        given(domainValidator.getOwnedItem(owner.getId(), itemA.getId())).willReturn(itemA);
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
        given(domainValidator.getOwnedItem(222L, itemA.getId())).willThrow(new ImsException(ErrorCode.ITEM_NOT_OWNED));

        // when & then
        assertThatThrownBy(() -> bomService.getBoms(222L, itemA.getId()))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ITEM_NOT_OWNED);
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
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ITEM_NOT_OWNED);
    }

    @Test
    @DisplayName("getFullBomTree - 단일 레벨: A → B(qty=2)")
    void getFullBomTree_singleLevel() {
        // given: owner의 BOM 전체를 한 번에 로드 (in-memory traversal)
        Bom bomAB = Bom.builder().id(10L).parent(itemA).child(itemB).quantity(2).build();
        given(bomRepository.findAllByParentOwnerId(owner.getId())).willReturn(List.of(bomAB));

        // when
        Map<Long, Long> result = bomService.getFullBomTree(itemA.getId(), owner.getId());

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(itemB.getId())).isEqualTo(2L);
    }

    @Test
    @DisplayName("getFullBomTree - 다단계: A → B(2) → C(3), C 누적 수량 = 6")
    void getFullBomTree_multiLevel() {
        // given: owner의 모든 BOM을 한 번에 로드
        Bom bomAB = Bom.builder().id(10L).parent(itemA).child(itemB).quantity(2).build();
        Bom bomBC = Bom.builder().id(11L).parent(itemB).child(itemC).quantity(3).build();
        given(bomRepository.findAllByParentOwnerId(owner.getId())).willReturn(List.of(bomAB, bomBC));

        // when
        Map<Long, Long> result = bomService.getFullBomTree(itemA.getId(), owner.getId());

        // then
        assertThat(result).containsEntry(itemB.getId(), 2L);
        assertThat(result).containsEntry(itemC.getId(), 6L);
    }

    @Test
    @DisplayName("getFullBomTree - DAG 공유 부품 합산: A→B(2), A→C(3), B→D(4), C→D(5) → D = 2×4+3×5 = 23")
    void getFullBomTree_dagSharedPart() {
        // given
        Item itemD = Item.builder().id(4L).owner(owner).itemCode("D").name("공유부품D").type(ItemType.PART).build();

        Bom bomAB = Bom.builder().id(11L).parent(itemA).child(itemB).quantity(2).build();
        Bom bomAC = Bom.builder().id(12L).parent(itemA).child(itemC).quantity(3).build();
        Bom bomBD = Bom.builder().id(13L).parent(itemB).child(itemD).quantity(4).build();
        Bom bomCD = Bom.builder().id(14L).parent(itemC).child(itemD).quantity(5).build();
        given(bomRepository.findAllByParentOwnerId(owner.getId())).willReturn(List.of(bomAB, bomAC, bomBD, bomCD));

        // when
        Map<Long, Long> result = bomService.getFullBomTree(itemA.getId(), owner.getId());

        // then
        assertThat(result.get(itemB.getId())).isEqualTo(2L);
        assertThat(result.get(itemC.getId())).isEqualTo(3L);
        assertThat(result.get(itemD.getId())).isEqualTo(23L); // 2×4 + 3×5
    }

    @Test
    @DisplayName("getFullBomTree - BOM 깊이 20 초과 시 BOM_DEPTH_EXCEEDED 예외")
    void getFullBomTree_depthExceeded() {
        // given: item100 → item101 → ... → item120 (21개 체인, 깊이 20에서 예외 발생)
        List<Bom> chain = new ArrayList<>();
        Item prev = Item.builder().id(100L).owner(owner).itemCode("I0").name("I0").type(ItemType.PRODUCT).build();
        for (int i = 1; i <= 20; i++) {
            Item next = Item.builder().id(100L + i).owner(owner).itemCode("I" + i).name("I" + i).type(ItemType.PART).build();
            chain.add(Bom.builder().parent(prev).child(next).quantity(1).build());
            prev = next;
        }
        given(bomRepository.findAllByParentOwnerId(owner.getId())).willReturn(chain);

        // when & then
        assertThatThrownBy(() -> bomService.getFullBomTree(100L, owner.getId()))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOM_DEPTH_EXCEEDED);
    }

    @Test
    @DisplayName("BOM 등록 실패 - child Item 소유자 아님")
    void addBom_childNotOwned() {
        // given
        BomCreateRequest request = new BomCreateRequest(itemB.getId(), 2);
        given(domainValidator.getOwnedItem(owner.getId(), itemA.getId())).willReturn(itemA);
        given(domainValidator.getOwnedItem(owner.getId(), itemB.getId()))
                .willThrow(new ImsException(ErrorCode.ITEM_NOT_OWNED));

        // when & then
        assertThatThrownBy(() -> bomService.addBom(owner.getId(), itemA.getId(), request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ITEM_NOT_OWNED);
    }

    @Test
    @DisplayName("BOM 삭제 실패 - BOM 없음")
    void deleteBom_notFound() {
        // given
        given(bomRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bomService.deleteBom(owner.getId(), 999L))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOM_NOT_FOUND);
    }
}
