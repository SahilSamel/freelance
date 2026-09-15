package com.freelance.template.utils;

import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.ITemplate;

import java.util.Map;

public final class TemplateRenderUtility {

    private TemplateRenderUtility() {}

    public static String render(AlertTemplateEntity template, Map<String, ?> data) {
        if (template == null) return "";
        return render(template.getContent(), data);
    }

    public static String render(ITemplate template, Map<String, ?> data) {
        if (template == null) return "";
        return render(template.getContent(), data);
    }

    public static String render(String templateContent, Map<String, ?> data) {
        if (templateContent == null) return "";
        if (data == null || data.isEmpty()) return templateContent;

        String rendered = templateContent;
        for (Map.Entry<String, ?> entry : data.entrySet()) {
            if (entry.getKey() != null) {
                String placeholder = "{{" + entry.getKey() + "}}";
                String replacement = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
                rendered = rendered.replace(placeholder, replacement);
            }
        }
        return rendered;
    }
}
