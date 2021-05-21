package net.minestom.code_generation.blocks;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.squareup.javapoet.*;
import net.minestom.code_generation.MinestomCodeGenerator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BlockGenerator extends MinestomCodeGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockGenerator.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final File blocksFile;
    private final File blockPropertyFile;
    private final File outputFolder;

    public BlockGenerator(@NotNull File blocksFile, @NotNull File blockPropertyFile, @NotNull File outputFolder) {
        this.blocksFile = blocksFile;
        this.blockPropertyFile = blockPropertyFile;
        this.outputFolder = outputFolder;
    }

    @Override
    public void generate() {
        if (!blocksFile.exists()) {
            LOGGER.error("Failed to find blocks.json.");
            LOGGER.error("Stopped code generation for blocks.");
            return;
        }
        if (!blockPropertyFile.exists()) {
            LOGGER.error("Failed to find block_properties.json.");
            LOGGER.error("Stopped code generation for block properties.");
            return;
        }

        if (!outputFolder.exists() && !outputFolder.mkdirs()) {
            LOGGER.error("Output folder for code generation does not exist and could not be created.");
            return;
        }
        if (!outputFolder.exists() && !outputFolder.mkdirs()) {
            LOGGER.error("Output folder for code generation does not exist and could not be created.");
            return;
        }
        // Important classes we use alot
        ClassName namespaceIDCN = ClassName.get("net.minestom.server.utils", "NamespaceID");
        ClassName blockPropertyCN = ClassName.get("net.minestom.server.instance.block", "BlockProperty");
        ClassName blockCN = ClassName.get("net.minestom.server.instance.block", "Block");
        ClassName blockImplCN = ClassName.get("net.minestom.server.instance.block", "BlockImpl");

        JsonArray blockProperties;
        try {
            blockProperties = GSON.fromJson(new JsonReader(new FileReader(blockPropertyFile)), JsonArray.class);
        } catch (FileNotFoundException e) {
            LOGGER.error("Failed to find block_properties.json.");
            LOGGER.error("Stopped code generation for block entities.");
            return;
        }
        ClassName blockPropertiesCN = ClassName.get("net.minestom.server.instance.block", "BlockProperties");
        // Particle
        TypeSpec.Builder blockPropertiesClass = TypeSpec.classBuilder(blockPropertiesCN)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                // Add @SuppressWarnings("unused")
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "unused").build())
                .addJavadoc("AUTOGENERATED by " + getClass().getSimpleName());
        // Add private constructor
        blockPropertiesClass.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .build()
        );
        // This stores the classes of the field names, e.g. WATERLOGGED --> Boolean
        Map<String, Class<?>> propertyClassMap = new HashMap<>();
        Map<String, String> propertyKeyMap = new HashMap<>();
        Map<String, String[]> propertyValueMap = new HashMap<>();
        // Use data
        for (JsonElement e : blockProperties) {
            JsonObject blockProperty = e.getAsJsonObject();

            String propertyName = blockProperty.get("name").getAsString();
            JsonArray blockValues = blockProperty.get("values").getAsJsonArray();
            JsonPrimitive firstElement = blockValues.get(0).getAsJsonPrimitive();

            Class<?> type;
            StringBuilder values = new StringBuilder();
            if (firstElement.isBoolean()) {
                type = Boolean.class;
                values = new StringBuilder("true, false");
            } else if (firstElement.isNumber()) {
                type = Integer.class;
                for (JsonElement blockValue : blockValues) {
                    int i = blockValue.getAsInt();
                    values.append(i).append(", ");
                }
                // Delete final ', '
                values.delete(values.lastIndexOf(","), values.length());
            } else {
                type = String.class;
                for (JsonElement blockValue : blockValues) {
                    String s = blockValue.getAsString();
                    values.append("\"").append(s).append("\", ");
                }
                // Delete final ', '
                values.delete(values.lastIndexOf(","), values.length());
            }
            String propertyKey = blockProperty.get("key").getAsString();

            propertyKeyMap.put(propertyName, propertyKey);
            propertyClassMap.put(propertyName, type);
            propertyValueMap.put(propertyName, values.toString().split(", "));
            // Adds the field to the main class
            // e.g. BlockProperty<Boolean> WATERLOGGED = new BlockProperty<Boolean>("waterlogged", true, false)
            blockPropertiesClass.addField(
                    FieldSpec.builder(
                            ParameterizedTypeName.get(blockPropertyCN, TypeName.get(type)),
                            propertyName
                    ).initializer(
                            "new $T<>($S, $L)",
                            blockPropertyCN,
                            propertyKey,
                            values
                    ).addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).build()
            );
        }

        JsonArray blocks;
        try {
            blocks = GSON.fromJson(new JsonReader(new FileReader(blocksFile)), JsonArray.class);
        } catch (FileNotFoundException e) {
            LOGGER.error("Failed to find blocks.json.");
            LOGGER.error("Stopped code generation for blocks.");
            return;
        }
        ClassName blocksCN = ClassName.get("net.minestom.server.instance.block", "Blocks");
        // Blocks class
        TypeSpec.Builder blocksClass = TypeSpec.classBuilder(blocksCN)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                // Add @SuppressWarnings("unused")
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "unused").build())
                .addJavadoc("AUTOGENERATED by " + getClass().getSimpleName());
        // Add private constructor
        blocksClass.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .build()
        );

        ClassName blockOld = ClassName.get("net.minestom.server.instance.block", "BlockOld");
        // BlockOld interface
        TypeSpec.Builder blockOldClass = TypeSpec.interfaceBuilder(blockOld)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(Deprecated.class).addMember("forRemoval", "$L", true).build())
                .addJavadoc("AUTOGENERATED by " + getClass().getSimpleName());

        // Use data
        for (JsonElement b : blocks) {
            JsonObject block = b.getAsJsonObject();

            String blockName = block.get("name").getAsString();
            // Handle the properties
            // Create a subclass for each Block and reference the main BlockProperty.
            JsonArray properties = block.get("properties").getAsJsonArray();
            if (properties.size() != 0) {
                // Create a subclass called "blockName"
                // e.g. subclass AIR in BlockProperties
                TypeSpec.Builder subClass = TypeSpec.classBuilder(blockPropertiesCN.nestedClass(blockName))
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .addJavadoc(
                                "Represents the $L {@link $T $L} that {@link $T#$N} can have:\n",
                                properties.size(),
                                blockPropertyCN,
                                properties.size() > 1 ? "properties" : "property",
                                blocksCN,
                                blockName
                        ).addJavadoc("<ul>\n");

                // Store a list of values for the getProperties() method.
                StringBuilder values = new StringBuilder();
                // Go through all properties the block has.
                for (JsonElement property : properties) {
                    String propertyName = property.getAsString();

                    // Add a static field that delegates to the BlockProperties static definition
                    FieldSpec.Builder field = FieldSpec.builder(
                            ParameterizedTypeName.get(blockPropertyCN, TypeName.get(propertyClassMap.get(propertyName))),
                            propertyName)
                            .initializer("$T.$N", blockPropertiesCN, propertyName)
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .addJavadoc("Definition: \"$L\" = [", propertyKeyMap.get(propertyName));
                    // Results in "key" = ["value1", "value2"...]
                    String[] propertyVals = propertyValueMap.get(propertyName);
                    for (int i = 0; i < propertyVals.length; i++) {
                        if (i == propertyVals.length - 1) {
                            field.addJavadoc("$L]", propertyVals[i].toLowerCase());
                        } else {
                            field.addJavadoc("$L, ", propertyVals[i].toLowerCase());
                        }
                    }
                    // Add field to subclass
                    subClass.addField(field.build());

                    values.append(propertyName).append(", ");
                    subClass.addJavadoc("<li>{@link $T#$N}</li>\n", blockPropertiesCN, propertyName);
                }
                subClass.addJavadoc("</ul>");
                // Delete final ', '
                values.delete(values.lastIndexOf(","), values.length());
                // Add a static method to get all the properties
                subClass.addMethod(
                        MethodSpec.methodBuilder("getProperties")
                                .returns(
                                        // List<BlockProperty<?>>
                                        ParameterizedTypeName.get(
                                                ClassName.get(List.class),
                                                // BlockProperty<?>
                                                ParameterizedTypeName.get(blockPropertyCN, TypeVariableName.get("?"))
                                        )
                                )
                                .addStatement(
                                        "return $T.of($L)",
                                        // If it has multiple properties --> Arrays.asList() else Collections.singletonList()
                                        ClassName.get(List.class),
                                        values
                                )
                                .addModifiers(Modifier.STATIC)
                                .build()
                );
                blockPropertiesClass.addType(subClass.build());
            }
            JsonArray states = block.getAsJsonArray("states");
            // Now handle the fields in Blocks.
            // If we don't have properties
            if (properties.size() == 0) {
                // This is a block like Stone that only has 1 BlockState.
                blocksClass.addField(
                        FieldSpec.builder(blockCN, blockName)
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                .initializer(
                                        // Blocks.STONE = new BlockImpl(NamespaceID.from("minecraft:stone"), 1, Collections.emptyList())
                                        "$T.create($T.from($S), (short) $L, (short) $L, (short) $L, (short) $L, $T.emptyList())",
                                        blockImplCN,
                                        namespaceIDCN,
                                        block.get("id").getAsString(),
                                        // Block id
                                        block.get("numericalID").getAsShort(),
                                        // First state id
                                        states.get(0).getAsJsonObject().get("id").getAsShort(),
                                        // Last state id
                                        states.get(states.size() - 1).getAsJsonObject().get("id").getAsShort(),
                                        // Default state id
                                        block.get("defaultBlockState").getAsShort(),
                                        ClassName.get(Collections.class)
                                )
                                .build()
                );
            } else {
                // This is a block that has multiple properties.
                blocksClass.addField(
                        FieldSpec.builder(blockCN, blockName)
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                .initializer(
                                        // Blocks.GRASS_BLOCK = new BlockImpl(NamespaceID.from("minecraft:grass_block"), 9, 8, varargsProperty)
                                        "$T.create($T.from($S), (short) $L, (short) $L, (short) $L, (short) $L, $T.$N.getProperties())",
                                        blockImplCN,
                                        namespaceIDCN,
                                        block.get("id").getAsString(),
                                        // Block id
                                        block.get("numericalID").getAsShort(),
                                        // First id
                                        block.getAsJsonArray("states").get(0).getAsJsonObject().get("id").getAsShort(),
                                        // Last state id
                                        states.get(states.size() - 1).getAsJsonObject().get("id").getAsShort(),
                                        // DefaultBlockStateId
                                        block.get("defaultBlockState").getAsShort(),
                                        blockPropertiesCN,
                                        blockName
                                )
                                .build()
                );
            }

            // TEMPORARY CODE FOR THE DEPRECATION OF BLOCK CONSTANTS
            // THE CREATED INTERFACE IS DEPRECATED AND WILL BE REMOVED EVENTUALLY
            blockOldClass.addField(
                    FieldSpec.builder(blockCN, blockName)
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .initializer("$T.$N", blocksCN, blockName)
                            .addAnnotation(AnnotationSpec.builder(Deprecated.class).addMember("forRemoval", "$L", true).build())
                            .addJavadoc(CodeBlock.builder().addStatement("Migrated to {@link $T#$N}", blocksCN, blockName).build())
                            .build()
            );
            // TEMPORARY CODE FOR THE DEPRECATION OF BLOCK CONSTANTS
        }
        writeFiles(
                Collections.singletonList(
                        JavaFile.builder("net.minestom.server.instance.block", blockOldClass.build())
                                .indent("    ")
                                .skipJavaLangImports(true)
                                .build()
                ),
                outputFolder
        );
        writeFiles(
                Collections.singletonList(
                        JavaFile.builder("net.minestom.server.instance.block", blockPropertiesClass.build())
                                .indent("    ")
                                .skipJavaLangImports(true)
                                .build()
                ),
                outputFolder
        );
        writeFiles(
                Collections.singletonList(
                        JavaFile.builder("net.minestom.server.instance.block", blocksClass.build())
                                .indent("    ")
                                .skipJavaLangImports(true)
                                .build()
                ),
                outputFolder
        );
    }
}
