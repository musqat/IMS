package com.ims.inventory.dto.response;

public record MaxProducibleResponse(
        Long itemId,
        String itemName,
        int maxQuantity
) {}
