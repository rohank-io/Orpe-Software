package com.orpe.consultants.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "models")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Models {

 @Id
 @Column(name = "model_no", length = 150, nullable = false,updatable = false)
 @NotBlank
 @Size(max = 150)
 private String modelNo;
}
