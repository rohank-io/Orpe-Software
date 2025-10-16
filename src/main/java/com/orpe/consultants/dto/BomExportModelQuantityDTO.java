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
	private Long bomId;
    private String modelNo;
    private BigDecimal quantity;
    private String status;
}
