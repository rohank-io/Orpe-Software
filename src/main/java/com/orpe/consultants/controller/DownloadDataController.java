package com.orpe.consultants.controller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Controller;
import org.springframework.util.function.ThrowingConsumer;
import org.springframework.web.bind.annotation.GetMapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orpe.consultants.dto.BomClaimDTO;
import com.orpe.consultants.dto.BomDataDTO;
import com.orpe.consultants.dto.BomExportModelQuantityDTO;
import com.orpe.consultants.dto.ExportDataDTO;
import com.orpe.consultants.dto.ImportDataDTO;
import com.orpe.consultants.service.BomClaimService;
import com.orpe.consultants.service.BomDataService;
import com.orpe.consultants.service.DraftWorksheetService;
import com.orpe.consultants.service.ExportDataService;
import com.orpe.consultants.service.ImportDataService;
import com.orpe.consultants.service.WorksheetService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DownloadDataController {

	private final ImportDataService importDataService;
	private final ExportDataService exportDataService;
	private final BomDataService bomService;
	private final BomClaimService bomClaimService;

	private final WorksheetService worksheetService;

	@GetMapping("/export/all-zip")
    public void exportAllAsZip(HttpServletResponse response) throws IOException {
        String zipFilename = "all-data-export.zip";

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipFilename + "\"");

        // Use streaming zip directly to response output stream (memory friendly)
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream(), StandardCharsets.UTF_8)) {

        	// 1) Import Data CSV (full fields)
        	List<ImportDataDTO> importRows = importDataService.findAll();
        	writeListAsCsvToZip(zos, "importdata.csv",
        	    new String[] {
        	        "importId", "beNo", "beDate", "beMonth", "beYear", "claimRefNo", "claimYear",
        	        "portCode", "countryOfOrigin", "supplierNameAddress", "itchsCode", "itemDescription",
        	        "bomPartNo", "altBoePartNo", "dbkPartNo", "quantity", "uom", "assessableValue",
        	        "bcdRate", "bcd", "swsRate", "sws", "addRate", "addDuty", "igstRate", "igst",
        	        "totalDuty", "notnNo", "notnEligibility", "qtyOpeningBalance", "qtyUsed",
        	        "closingBalance", "stockWiseEligibility", "dutyClaimedAmt", "clientName", "exportModels"
        	    },
        	    (writer) -> {
        	        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        	        for (ImportDataDTO dto : importRows) {
        	            String beDate = dto.getBeDate() != null ? dto.getBeDate().format(df) : "";

        	            // exportModels: join elements (use toString() as fallback)
        	            String exportModelsJoined = "";
        	            if (dto.getExportModels() != null && !dto.getExportModels().isEmpty()) {
        	                exportModelsJoined = dto.getExportModels().stream()
        	                    .map(em -> {
        	                        try {
        	                            return em == null ? "" : em.toString();
        	                        } catch (Exception ex) {
        	                            return "";
        	                        }
        	                    })
        	                    .filter(s -> s != null && !s.isEmpty())
        	                    .collect(Collectors.joining("; "));
        	            }

        	            String[] cols = new String[] {
        	                safe(dto.getImportId()),
        	                safe(dto.getBeNo()),
        	                safe(beDate),
        	                safe(dto.getBeMonth()),
        	                dto.getBeYear() != null ? dto.getBeYear().toString() : "",
        	                safe(dto.getClaimRefNo()),
        	                safe(dto.getClaimYear()),
        	                safe(dto.getPortCode()),
        	                safe(dto.getCountryOfOrigin()),
        	                safe(dto.getSupplierNameAddress()),
        	                safe(dto.getItchsCode()),
        	                safe(dto.getItemDescription()),
        	                safe(dto.getBomPartNo()),
        	                safe(dto.getAltBoePartNo()),
        	                safe(dto.getDbkPartNo()),
        	                safeNumber(dto.getQuantity()),
        	                safe(dto.getUom()),
        	                safeNumber(dto.getAssessableValue()),
        	                safeNumber(dto.getBcdRate()),
        	                safeNumber(dto.getBcd()),
        	                safeNumber(dto.getSwsRate()),
        	                safeNumber(dto.getSws()),
        	                safeNumber(dto.getAddRate()),
        	                safeNumber(dto.getAddDuty()),
        	                safeNumber(dto.getIgstRate()),
        	                safeNumber(dto.getIgst()),
        	                safeNumber(dto.getTotalDuty()),
        	                safe(dto.getNotnNo()),
        	                safe(dto.getNotnEligibility()),
        	                safeNumber(dto.getQtyOpeningBalance()),
        	                safeNumber(dto.getQtyUsed()),
        	                safeNumber(dto.getClosingBalance()),
        	                safe(dto.getStockWiseEligibility() == null ? "" : dto.getStockWiseEligibility().toString()),
        	                safeNumber(dto.getDutyClaimedAmt()),
        	                safe(dto.getClientName()),
        	                safe(exportModelsJoined)
        	            };

        	            writer.println(csvLine(cols));
        	        }
        	    });
        	
        	
        	// 2) Export Data CSV (full fields)
        	List<ExportDataDTO> exportRows = exportDataService.findAll();

        	writeListAsCsvToZip(zos, "exportdata.csv",
        	    new String[] {
        	        "exportId", "sbNo", "sbDate", "portCode", "customerName",
        	        "leoDate", "claimRefNo", "claimYear", "schemeDescription",
        	        "dbkSno", "dbkApplicability", "rate", "airGivenInSb", "airAmount",
        	        "difference", "totalDbk", "sbr", "aroNo", "aroDate",
        	        "aroFileNo", "aroFileDate", "brcNo", "netRealisedValue",
        	        "netRealisedCurrency", "sbUtilization", "invoiceNo",
        	        "invoiceDate", "modelNo", "productType", "hsCode",
        	        "modelDescription", "quantity", "unit", "invoiceValueFcc",
        	        "currencyCode", "fobInr", "pmvPerQty", "pmvActual",
        	        "createdAt", "updatedAt", "clientName"
        	    },
        	    (writer) -> {
        	        DateTimeFormatter dfDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        	        DateTimeFormatter dfDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        	        for (ExportDataDTO dto : exportRows) {
        	            String sbDate = dto.getSbDate() != null ? dto.getSbDate().format(dfDate) : "";
        	            String leoDate = dto.getLeoDate() != null ? dto.getLeoDate().format(dfDate) : "";
        	            String aroDate = dto.getAroDate() != null ? dto.getAroDate().format(dfDate) : "";
        	            String aroFileDate = dto.getAroFileDate() != null ? dto.getAroFileDate().format(dfDate) : "";
        	            String invoiceDate = dto.getInvoiceDate() != null ? dto.getInvoiceDate().format(dfDate) : "";

        	            String createdAt = dto.getCreatedAt() != null ? dto.getCreatedAt().format(dfDateTime) : "";
        	            String updatedAt = dto.getUpdatedAt() != null ? dto.getUpdatedAt().format(dfDateTime) : "";

        	            String[] cols = new String[] {
        	                safe(dto.getExportId()),
        	                safe(dto.getSbNo()),
        	                safe(sbDate),
        	                safe(dto.getPortCode()),
        	                safe(dto.getCustomerName()),

        	                safe(leoDate),
        	                safe(dto.getClaimRefNo()),
        	                safe(dto.getClaimYear()),
        	                safe(dto.getSchemeDescription()),

        	                safe(dto.getDbkSno()),
        	                safe(dto.getDbkApplicability()),
        	                safeNumber(dto.getRate()),
        	                safeNumber(dto.getAirGivenInSb()),
        	                safeNumber(dto.getAirAmount()),

        	                safeNumber(dto.getDifference()),
        	                safeNumber(dto.getTotalDbk()),
        	                safeNumber(dto.getSbr()),
        	                safe(dto.getAroNo()),
        	                safe(aroDate),

        	                safe(dto.getAroFileNo()),
        	                safe(aroFileDate),
        	                safe(dto.getBrcNo()),
        	                safeNumber(dto.getNetRealisedValue()),

        	                safe(dto.getNetRealisedCurrency()),
        	                safe(dto.getSbUtilization()),
        	                safe(dto.getInvoiceNo()),

        	                safe(invoiceDate),
        	                safe(dto.getModelNo()),
        	                safe(dto.getProductType()),
        	                safe(dto.getHsCode()),

        	                safe(dto.getModelDescription()),
        	                safeNumber(dto.getQuantity()),
        	                safe(dto.getUnit()),
        	                safeNumber(dto.getInvoiceValueFcc()),

        	                safe(dto.getCurrencyCode()),
        	                safeNumber(dto.getFobInr()),
        	                safeNumber(dto.getPmvPerQty()),
        	                safeNumber(dto.getPmvActual()),

        	                safe(createdAt),
        	                safe(updatedAt),
        	                safe(dto.getClientName())
        	            };

        	            writer.println(csvLine(cols));
        	        }
        	    }
        	);
        	
        	
        	
        	// BOM CSV: one row per BomDataDTO
        	List<BomDataDTO> bomRows = bomService.findAllForExport();

        	writeListAsCsvToZip(zos, "bom.csv",
        	    new String[] {
        	        "bomId", "claimRefNo", "claimYear", "materialDesc",
        	        "bomPartNo", "alternateBoePartNo", "dbkPartNo", "importedIndigenous",
        	        "unit", "grandTotal", "netWeightKg", "createdAt", "clientName"
        	    },
        	    (writer) -> {
        	        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        	        for (BomDataDTO dto : bomRows) {
        	            String createdAt = dto.getCreatedAt() != null ? dto.getCreatedAt().format(dtf) : "";
        	            String[] cols = new String[] {
        	                safe(dto.getBomId()),
        	                safe(dto.getClaimRefNo()),
        	                safe(dto.getClaimYear()),
        	                safe(dto.getMaterialDesc()),
        	                safe(dto.getBomPartNo()),
        	                safe(dto.getAlternateBoePartNo()),
        	                safe(dto.getDbkPartNo()),
        	                safe(dto.getImportedIndigenous()),
        	                safe(dto.getUnit()),
        	                safeNumber(dto.getGrandTotal()),
        	                safeNumber(dto.getNetWeightKg()),
        	                safe(createdAt),
        	                safe(dto.getClientName())
        	            };
        	            writer.println(csvLine(cols));
        	        }
        	    });
        	
        	
        	// BOM Export Models CSV: one row per BomExportModelQuantityDTO (child)
        	writeListAsCsvToZip(zos, "bom_export_models.csv",
        		    new String[] { "id", "bomId", "claimRefNo", "claimYear", "modelNo", "quantity", "status" },
        		    (writer) -> {
        		        for (BomDataDTO dto : bomRows) {
        		            List<BomExportModelQuantityDTO> ems = dto.getExportModels();
        		            if (ems == null || ems.isEmpty()) {
        		                // optional: you can write a placeholder row indicating no children
        		                continue;
        		            }
        		            for (BomExportModelQuantityDTO em : ems) {
        		                String[] cols = new String[] {
        		                    safe(em.getId()),
        		                    safe(dto.getBomId()),
        		                    safe(dto.getClaimRefNo()),
        		                    safe(dto.getClaimYear()),
        		                    safe(em.getModelNo()),
        		                    safeNumber(em.getQuantity()),
        		                    safe(em.getStatus())
        		                };
        		                writer.println(csvLine(cols));
        		            }
        		        }
        		    });
        	
        	
        	
        	
        	// BOMCLAIM CSV: one row per BomClaimDTO
        	List<BomClaimDTO> bomClaimRows = bomClaimService.findAll(); // or findAllForExport()

        	writeListAsCsvToZip(zos, "Bom2.csv",
        	    new String[] {
        	        "claimId", "claimRefNo", "claimYear", "materialDescription",
        	        "bomPartNo", "altBoePartNo", "dbkPartNo", "importedOrIndigenous",
        	        "unit", "boeNo", "usedQty", "exportModelNo", "sbNo",
        	        "clientName", "createdAt", "updatedAt"
        	    },
        	    (writer) -> {
        	        DateTimeFormatter dtfDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        	        for (BomClaimDTO dto : bomClaimRows) {
        	            String createdAt = dto.getCreatedAt() != null ? dto.getCreatedAt().format(dtfDateTime) : "";
        	            String updatedAt = dto.getUpdatedAt() != null ? dto.getUpdatedAt().format(dtfDateTime) : "";

        	            String[] cols = new String[] {
        	                safe(dto.getClaimId()),
        	                safe(dto.getClaimRefNo()),
        	                safe(dto.getClaimYear()),
        	                safe(dto.getMaterialDescription()),
        	                safe(dto.getBomPartNo()),
        	                safe(dto.getAltBoePartNo()),
        	                safe(dto.getDbkPartNo()),
        	                safe(dto.getImportedOrIndigenous()),
        	                safe(dto.getUnit()),
        	                safe(dto.getBoeNo()),
        	                safeNumber(dto.getUsedQty()),
        	                safe(dto.getExportModelNo()),
        	                safe(dto.getSbNo()),
        	                safe(dto.getClientName()),
        	                safe(createdAt),
        	                safe(updatedAt)
        	            };

        	            writer.println(csvLine(cols));
        	        }
        	    });


        	
        	
        	




           

            // finish ZIP (try-with-resources closes zos)
        }
    }

    /**
     * Helper: open a ZIP entry and write a header row and content via the provided writerConsumer.
     * WriterConsumer writes content lines (already escaped) using println().
     */
    private void writeListAsCsvToZip(ZipOutputStream zos, String entryName, String[] headers,
                                     ThrowingConsumer<PrintWriter> writerConsumer) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);

        // Do NOT close the writer (closing would close underlying stream), only flush and closeEntry
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(zos, StandardCharsets.UTF_8));
        PrintWriter writer = new PrintWriter(bw, true);

        // Optionally write UTF-8 BOM for Excel recognition (uncomment if desired)
        // writer.write('\uFEFF');

        // header
        writer.println(csvLine(headers));

        // data
        writerConsumer.accept(writer);

        writer.flush();
        zos.closeEntry(); // important
        // do NOT close writer here
    }

    // functional interface to allow lambda throwing IOException
    @FunctionalInterface
    private interface ThrowingConsumer<T> {
        void accept(T t) throws IOException;
    }

    // CSV helpers
    private String csvLine(String... cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escapeCsv(cells[i]));
        }
        return sb.toString();
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        String v = s;
        if (v.contains("\"")) v = v.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\n") || v.contains("\r") || v.contains("\"")) {
            return "\"" + v + "\"";
        } else {
            return v;
        }
    }

    private String safe(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private String safeNumber(Number n) {
        return n == null ? "" : n.toString();
    }

}
