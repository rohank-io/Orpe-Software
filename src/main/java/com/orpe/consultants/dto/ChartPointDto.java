package com.orpe.consultants.dto;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChartPointDto {
    private String period;      // e.g. "2025-01-01" or "Jan"
    private Long imports;       // import count
    private Long exports;       // export count
    private Long sbCount;       // optional (your SB count)
}
