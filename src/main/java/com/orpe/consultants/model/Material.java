package com.orpe.consultants.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "materials")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Material {

 @Id
 @Column(name = "bom_part_no", length = 50, nullable = false, updatable = false)
 @NotBlank
 @Size(max = 50)
 private String bomPartNo;
}
