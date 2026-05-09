package com.ims.inventory.dto.response;

import java.util.List;

public record ShortageItemResponse(
        Long itemId,
        String itemCode,
        String itemName,
        List<PartShortageDto> shortages
) {}
