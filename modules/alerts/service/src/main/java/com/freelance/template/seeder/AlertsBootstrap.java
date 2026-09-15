package com.freelance.template.seeder;

import com.freelance.alerts.attribute.IDomainAttributeResolver;
import com.freelance.attribute.AttributeResolutionEngine;
import com.freelance.alerts.template.ITemplate;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class AlertsBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(AlertsBootstrap.class);

    public static final String MANDATED_TEMPLATE_PACKAGE = "com.freelance.alerts.templates";

    public static final String MANDATED_ATTRIBUTE_PACKAGE = "com.freelance.alerts.attribute";

    private AlertsBootstrap() {}

    public static void init() {
        logger.info("[AlertsBootstrap] Initializing Alerts & Messaging module...");

        seedTemplateCategories();
        scanAndRegisterAttributeResolvers();
        scanAndSeedCodeTemplates();

        logger.info("[AlertsBootstrap] Alerts & Messaging module initialized successfully.");
    }

    private static void seedTemplateCategories() {
        try {
            new TemplateCategorySeeder().seed();
        } catch (Exception e) {
            logger.error("[AlertsBootstrap] Failed to seed template categories", e);
        }
    }

    private static void scanAndRegisterAttributeResolvers() {
        logger.info("[AlertsBootstrap] Scanning mandated package '{}' for IDomainAttributeResolver implementations...", MANDATED_ATTRIBUTE_PACKAGE);

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .forPackages(MANDATED_ATTRIBUTE_PACKAGE)
                .filterInputsBy(new FilterBuilder().includePackage(MANDATED_ATTRIBUTE_PACKAGE))
                .setScanners(Scanners.SubTypes));

        Set<Class<? extends IDomainAttributeResolver>> classes = reflections.getSubTypesOf(IDomainAttributeResolver.class);
        int registeredCount = 0;

        for (Class<? extends IDomainAttributeResolver> clazz : classes) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }

            if (!clazz.getPackageName().startsWith(MANDATED_ATTRIBUTE_PACKAGE)) {
                logger.warn("[AlertsBootstrap] Class '{}' implements IDomainAttributeResolver but is NOT in mandated package '{}'. Skipping.",
                        clazz.getName(), MANDATED_ATTRIBUTE_PACKAGE);
                continue;
            }

            try {
                IDomainAttributeResolver resolver = clazz.getDeclaredConstructor().newInstance();
                AttributeResolutionEngine.getInstance().register(resolver);
                registeredCount++;
                logger.info("[AlertsBootstrap] Auto-registered IDomainAttributeResolver '{}' for domain '{}'",
                        clazz.getSimpleName(), resolver.getDomain());
            } catch (Exception e) {
                logger.error("[AlertsBootstrap] Failed to instantiate IDomainAttributeResolver class '{}'. Ensure it has a public no-arg constructor.",
                        clazz.getName(), e);
                throw new IllegalStateException("Failed to instantiate IDomainAttributeResolver class: " + clazz.getName(), e);
            }
        }

        logger.info("[AlertsBootstrap] Discovered and registered {} IDomainAttributeResolver(s).", registeredCount);
    }

    private static void scanAndSeedCodeTemplates() {
        logger.info("[AlertsBootstrap] Scanning mandated package '{}' for ITemplate implementations...", MANDATED_TEMPLATE_PACKAGE);

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .forPackages(MANDATED_TEMPLATE_PACKAGE)
                .filterInputsBy(new FilterBuilder().includePackage(MANDATED_TEMPLATE_PACKAGE))
                .setScanners(Scanners.SubTypes));

        Set<Class<? extends ITemplate>> classes = reflections.getSubTypesOf(ITemplate.class);
        List<ITemplate> templates = new ArrayList<>();

        for (Class<? extends ITemplate> clazz : classes) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }

            if (!clazz.getPackageName().startsWith(MANDATED_TEMPLATE_PACKAGE)) {
                logger.warn("[AlertsBootstrap] Class '{}' implements ITemplate but is NOT in mandated package '{}'. Skipping.",
                        clazz.getName(), MANDATED_TEMPLATE_PACKAGE);
                continue;
            }

            if (clazz.isEnum()) {
                ITemplate[] constants = clazz.getEnumConstants();
                if (constants != null) {
                    templates.addAll(Arrays.asList(constants));
                }
            } else {
                try {
                    ITemplate instance = clazz.getDeclaredConstructor().newInstance();
                    templates.add(instance);
                } catch (Exception e) {
                    logger.error("[AlertsBootstrap] Failed to instantiate ITemplate class '{}'. Ensure it has a public no-arg constructor.",
                            clazz.getName(), e);
                    throw new IllegalStateException("Failed to instantiate ITemplate class: " + clazz.getName(), e);
                }
            }
        }

        logger.info("[AlertsBootstrap] Discovered {} code-defined template(s) in '{}'. Seeding...", templates.size(), MANDATED_TEMPLATE_PACKAGE);
        try {
            TemplateSeeder seeder = new TemplateSeeder();
            seeder.seed(templates);
        } catch (Exception e) {
            logger.error("[AlertsBootstrap] Failed to seed code templates", e);
        }
    }
}