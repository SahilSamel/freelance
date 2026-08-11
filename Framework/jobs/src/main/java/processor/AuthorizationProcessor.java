package processor;

import com.freelance.rest.app.endpoint.annotation.Authorization;
import com.freelance.authorization.SystemRoles;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.freelance.rest.app.endpoint.annotation.Authorization")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class AuthorizationProcessor extends AbstractProcessor {

    private Messager messager;
    private final List<String> collectedJsonEntries = new ArrayList<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        // 1. Collect entries during normal processing rounds
        if (!roundEnv.processingOver()) {
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(Authorization.class);

            if (!annotatedElements.isEmpty()) {
                logNote(String.format("Found %d @Authorization element(s) in current round.", annotatedElements.size()));
            }

            for (Element element : annotatedElements) {
                Authorization auth = element.getAnnotation(Authorization.class);
                if (auth != null) {
                    logNote(String.format("Processing element: %s (Key: '%s')", element.getSimpleName(), auth.key()));

                    String key = escapeJson(auth.key());
                    String name = escapeJson(auth.name());
                    String description = escapeJson(auth.description());
                    String group = escapeJson(auth.group());

                    // Check for NONE default, otherwise fetch enum name
                    String roleStr = (auth.role() == SystemRoles.NONE) ? "" : auth.role().name();
                    String role = escapeJson(roleStr);

                    String entry = String.format(
                            "{\"key\":\"%s\",\"name\":\"%s\",\"description\":\"%s\",\"group\":\"%s\",\"role\":\"%s\"}",
                            key, name, description, group, role
                    );
                    collectedJsonEntries.add(entry);
                }
            }
        }
        // 2. Write out accumulated elements on the final round
        else {
            if (!collectedJsonEntries.isEmpty()) {
                writeJsonFile();
            } else {
                logWarning("No @Authorization entries collected to write.");
            }
        }

        return true;
    }

    private void writeJsonFile() {
        try {
            FileObject resourceFile = processingEnv.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT,
                    "",
                    "authorizations.json"
            );

            logNote(String.format("Writing fresh JSON file with %d entries to: %s",
                    collectedJsonEntries.size(), resourceFile.toUri()));

            try (Writer writer = resourceFile.openWriter()) {
                writer.write("[\n");
                for (int i = 0; i < collectedJsonEntries.size(); i++) {
                    writer.write("  " + collectedJsonEntries.get(i));
                    if (i < collectedJsonEntries.size() - 1) {
                        writer.write(",");
                    }
                    writer.write("\n");
                }
                writer.write("]\n");
            }

            logNote("Successfully generated authorizations.json!");
        } catch (IOException e) {
            logError("Failed to write authorization metadata: " + e.getMessage());
        }
    }

    private void logNote(String message) {
        messager.printMessage(Diagnostic.Kind.NOTE, "[AuthorizationProcessor] " + message);
    }

    private void logWarning(String message) {
        messager.printMessage(Diagnostic.Kind.WARNING, "[AuthorizationProcessor] " + message);
    }

    private void logError(String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, "[AuthorizationProcessor] " + message);
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}