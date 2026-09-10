package com.custody.reporting.report.impl;

import com.custody.audit.dto.response.AuditEventResponse;
import com.custody.reporting.enums.ReportFormat;
import com.custody.reporting.report.ReportRenderer;
import com.custody.reporting.utils.ReportColumns;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
class ExcelRenderer implements ReportRenderer {
  public ReportFormat format() {
    return ReportFormat.XLSX;
  }

  public byte[] render(List<AuditEventResponse> events, String criteria) {
    try (var workbook = new XSSFWorkbook();
        var out = new ByteArrayOutputStream()) {
      Sheet meta = workbook.createSheet("Criteria");
      row(meta, 0, List.of("Selection criteria", criteria));
      row(
          meta,
          1,
          List.of("Result", events.isEmpty() ? "No activity" : "Events: " + events.size()));
      Sheet sheet = workbook.createSheet("Audit evidence");
      row(sheet, 0, ReportColumns.HEADERS);
      int rowIndex = 1;
      for (AuditEventResponse auditEventResponse : events)
        for (var values : ReportColumns.rows(auditEventResponse)) row(sheet, rowIndex++, values);
      sheet.createFreezePane(0, 1);
      for (int columnIndex = 0; columnIndex < ReportColumns.HEADERS.size(); columnIndex++)
        sheet.setColumnWidth(columnIndex, 24 * 256);
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException iOException) {
      throw new IllegalStateException("Report rendering failed", iOException);
    }
  }

  private void row(Sheet sheet, int index, List<String> values) {
    Row row = sheet.createRow(index);
    for (int columnIndex = 0; columnIndex < values.size(); columnIndex++)
      row.createCell(columnIndex, CellType.STRING)
          .setCellValue(Objects.toString(values.get(columnIndex), ""));
  }
}
