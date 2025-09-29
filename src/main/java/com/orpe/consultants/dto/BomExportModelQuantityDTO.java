package com.orpe.consultants.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomExportModelQuantityDTO {
	
	private Long id;
    private String modelNo;
    private BigDecimal quantity;
}
