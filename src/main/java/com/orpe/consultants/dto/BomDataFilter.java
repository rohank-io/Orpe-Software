package com.orpe.consultants.dto;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomDataFilter {
	
	private Long bomId;
    private String claimRefNo;
    private String claimYear;
    private String materialDesc;
    private String bomPartNo;

}
