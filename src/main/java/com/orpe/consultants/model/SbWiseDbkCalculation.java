package com.orpe.consultants.model;



import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
    name = "tbl_sb_dbk_calculation",
    indexes = {
        @Index(name = "idx_sbno", columnList = "shipping_bill_no"),
        @Index(name = "idx_portcode", columnList = "port_code"),
        @Index(name = "idx_sbdate", columnList = "shipping_bill_date"),
        @Index(name = "idx_dbk_sno", columnList = "dbk_sno"),
        @Index(name = "idx_port_sbno", columnList = "port_code, shipping_bill_no"),
        @Index(name = "idx_import_qty", columnList = "imp_quantity"),
        @Index(name = "idx_value_addition", columnList = "value_addition")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SbWiseDbkCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dbk_calc_id", nullable = false, updatable = false)
    private Long dbkCalcId;

    @Column(name = "port_code", length = 50)
    private String portCode;

    @Column(name = "shipping_bill_no", length = 100)
    private String shippingBillNo;

    @Column(name = "shipping_bill_date")
    private LocalDate shippingBillDate;

    @Column(name = "leo_date")
    private LocalDate leoDate;

    @Column(name = "export_description", length = 500)
    private String exportDescription;

    @Column(name = "quantity", precision = 18, scale = 6)
    private BigDecimal quantity;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "dbk_sno", length = 100)
    private String dbkSno;

    @Column(name = "fob_value", precision = 18, scale = 2)
    private BigDecimal fobValue;

    @Column(name = "pmv_value", precision = 18, scale = 2)
    private BigDecimal pmvValue;

    @Column(name = "imp_quantity", precision = 18, scale = 6)
    private BigDecimal importQuantity;

    @Column(name = "consumption_per_export_qty", precision = 18, scale = 6)
    private BigDecimal consumptionPerExportQty;

    @Column(name = "total_duty", precision = 18, scale = 2)
    private BigDecimal totalDuty;

    @Column(name = "total_cif_value", precision = 18, scale = 2)
    private BigDecimal totalCifValue;

    @Column(name = "dbk_amount", precision = 18, scale = 2)
    private BigDecimal dbkAmount;

    @Column(name = "cif_value", precision = 18, scale = 2)
    private BigDecimal cifValue;

    @Column(name = "air_rate", precision = 18, scale = 6)
    private BigDecimal airRate;

    @Column(name = "air_amount", precision = 18, scale = 2)
    private BigDecimal airAmount;

    @Column(name = "sbr", precision = 18, scale = 2)
    private BigDecimal sbr;

    @Column(name = "four_fifth_brod_main_claim", precision = 18, scale = 2)
    private BigDecimal fourFifthOfBrodMainClaim;

    @Column(name = "diff_brod_air", precision = 18, scale = 2)
    private BigDecimal diffBrodAndAir;

    @Column(name = "value_addition", precision = 18, scale = 6)
    private BigDecimal valueAddition;
}
