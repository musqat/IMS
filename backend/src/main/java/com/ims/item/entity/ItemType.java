package com.ims.item.entity;

public enum ItemType {
    FINISHED,  // 완성품
    PART,      // 부품
    SEMI       // 반제품 (상위 완성품의 하위이면서 다른 부품의 상위 가능)
}
