package com.niyiment.agilebutler.decision.service;

import com.itextpdf.html2pdf.HtmlConverter;
import com.niyiment.agilebutler.common.exception.BusinessException;
import com.niyiment.agilebutler.common.exception.ResourceNotFoundException;
import com.niyiment.agilebutler.decision.dto.response.OptionResponse;
import com.niyiment.agilebutler.decision.dto.response.SessionResponse;
import com.niyiment.agilebutler.decision.model.DecisionSession;
import com.niyiment.agilebutler.decision.repository.DecisionSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Generates PDF and Markdown exports of closed decision sessions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneId.of("UTC"));
    private final DecisionSessionRepository sessionRepository;
    private final DecisionSessionService sessionService;

    @Transactional(readOnly = true)
    public byte[] exportAsPdf(UUID sessionId) {
        SessionResponse session = getClosedSession(sessionId);
        String markdown = buildMarkdown(session);

        return renderPdf(session.title(), markdown);
    }

    @Transactional(readOnly = true)
    public String exportAsMarkdown(UUID sessionId) {
        return buildMarkdown(getClosedSession(sessionId));
    }

    private String buildMarkdown(SessionResponse s) {
        var sb = new StringBuilder();

        sb.append("# ").append(s.title()).append("\n\n");

        if (s.description() != null && !s.description().isBlank()) {
            sb.append("> ").append(s.description().replace("\n", "\n> ")).append("\n\n");
        }

        sb.append("| Field | Value |\n");
        sb.append("|---|---|\n");
        sb.append("| **Type** | ").append(pretty(s.sessionType())).append(" / ")
                .append(pretty(s.decisionType())).append(" |\n");
        sb.append("| **Status** | ").append(pretty(s.status())).append(" |\n");
        sb.append("| **Anonymous** | ").append(s.anonymous() ? "Yes" : "No").append(" |\n");
        sb.append("| **Participants** | ").append(s.participantCount()).append(" |\n");
        sb.append("| **Total votes** | ").append(s.totalVotes()).append(" |\n");
        if (s.closedAt() != null) {
            sb.append("| **Closed at** | ").append(FMT.format(s.closedAt())).append(" |\n");
        }
        sb.append("\n");

        sb.append("## Results\n\n");
        sb.append("| Option | Votes | % |\n");
        sb.append("|---|---|---|\n");

        // Sort by votes descending so the winner is always first
        s.options().stream()
                .sorted((a, b) -> Long.compare(b.voteCount(), a.voteCount()))
                .forEach(opt -> sb.append("| ")
                        .append(opt.optionText()).append(" | ")
                        .append(opt.voteCount()).append(" | ")
                        .append(String.format("%.1f%%", opt.percentage())).append(" |\n"));

        if (!s.options().isEmpty()) {
            var winner = s.options().stream()
                    .max(java.util.Comparator.comparingLong(OptionResponse::voteCount))
                    .map(OptionResponse::optionText)
                    .orElse("—");
            sb.append("\n**🏆 Decision: ").append(winner).append("**\n");
        }

        sb.append("\n---\n");
        sb.append("*Exported from Agile Butler*\n");

        return sb.toString();
    }

    private byte[] renderPdf(String title, String markdownBody) {
        try (var baos = new ByteArrayOutputStream()) {
            // Convert Markdown-ish body to simple HTML for iText
            String html = convertToHtml(title, markdownBody);
            HtmlConverter.convertToPdf(html, baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("PDF generation failed for session export: {}", e.getMessage(), e);
            throw new BusinessException("Failed to generate PDF: " + e.getMessage());
        }
    }

    private String convertToHtml(String title, String markdownBody) {
        var sb = new StringBuilder();
        sb.append("<html><head><style>")
                .append("body { font-family: sans-serif; margin: 40px; color: #333; }")
                .append("h1 { color: #2c3e50; border-bottom: 2px solid #eee; padding-bottom: 10px; }")
                .append("h2 { color: #34495e; margin-top: 30px; }")
                .append("table { width: 100%; border-collapse: collapse; margin: 20px 0; }")
                .append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }")
                .append("th { background-color: #f8f9fa; }")
                .append("blockquote { font-style: italic; color: #555; border-left: 5px solid #eee; padding-left: 15px; margin: 20px 0; }")
                .append(".footer { margin-top: 50px; font-size: 0.8em; color: #777; border-top: 1px solid #eee; padding-top: 10px; }")
                .append(".winner { font-weight: bold; font-size: 1.2em; color: #27ae60; margin: 20px 0; }")
                .append("</style></head><body>");

        sb.append("<h1>").append(escapeHtml(title)).append("</h1>");

        for (String line : markdownBody.split("\n")) {
            if (line.startsWith("# ")) continue; // Skip main title handled above
            
            if (line.startsWith("## ")) {
                sb.append("<h2>").append(escapeHtml(line.substring(3))).append("</h2>");
            } else if (line.startsWith("> ")) {
                sb.append("<blockquote>").append(escapeHtml(line.substring(2))).append("</blockquote>");
            } else if (line.startsWith("|") && line.contains("|---|")) {
                // Skip table header separator
                continue;
            } else if (line.startsWith("|")) {
                // Basic table parsing
                if (!sb.toString().trim().endsWith("</table>") && !sb.toString().trim().endsWith("</tr>")) {
                    sb.append("<table>");
                }
                sb.append("<tr>");
                String[] cells = line.split("\\|");
                for (int i = 1; i < cells.length; i++) {
                    String tag = sb.toString().contains("<table><tr>") ? "td" : "th"; 
                    // Simple heuristic: first row after <table> is header if it wasn't skipped
                    // But our markdown already has headers.
                    sb.append("<td>").append(escapeHtml(cells[i].trim())).append("</td>");
                }
                sb.append("</tr>");
                // We'll close the table if the next line isn't a table line (handled below)
            } else if (line.startsWith("**🏆 Decision:")) {
                sb.append("<div class='winner'>").append(escapeHtml(line.replaceAll("\\*\\*", ""))).append("</div>");
            } else if (line.startsWith("---")) {
                sb.append("<hr/>");
            } else if (line.startsWith("*Exported")) {
                sb.append("<div class='footer'>").append(escapeHtml(line.replaceAll("\\*", ""))).append("</div>");
            } else if (!line.isBlank()) {
                if (sb.toString().endsWith("</tr>")) {
                    sb.append("</table>");
                }
                sb.append("<p>").append(escapeHtml(line)).append("</p>");
            }
        }
        
        if (sb.toString().endsWith("</tr>")) {
            sb.append("</table>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private SessionResponse getClosedSession(UUID sessionId) {
        DecisionSession session = sessionRepository.findByIdWithDetails(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("DecisionSession", sessionId));
        if (session.isOpen()) {
            throw new BusinessException(
                    "Cannot export an active session — close it first so results are final");
        }
        return sessionService.buildSessionResponse(session);
    }

    private String pretty(Object e) {
        return e.toString().replace("_", " ");
    }
}