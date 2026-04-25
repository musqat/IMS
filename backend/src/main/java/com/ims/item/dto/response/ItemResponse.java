package com.ims.item.dto.response;

import com.ims.item.entity.Item;
import com.ims.item.entity.ItemType;

public record ItemResponse(
        Long id,
        String itemCode,
        String name,
        ItemType type,
        String description
) {
    public static ItemResponse from(Item item) {
        return new ItemResponse(
                item.getId(),
                item.getItemCode(),
                item.getName(),
                item.getType(),
                item.getDescription()
        );
    }
}
