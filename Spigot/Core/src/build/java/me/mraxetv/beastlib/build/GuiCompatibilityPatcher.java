package me.mraxetv.beastlib.build;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Build-only compatibility overlay for the upstream GUI implementation classes. */
public final class GuiCompatibilityPatcher {
    static final String ORIGINAL = "dev/triumphteam/";
    static final String RELOCATED = "me/mraxetv/beastlib/lib/tgui/";
    static final String HUMAN = "Lorg/bukkit/entity/HumanEntity;";
    static final String PLAYER = "Lorg/bukkit/entity/Player;";
    static final String[] CLASSES = {"gui/guis/BaseGui", "gui/guis/PaginatedGui", "gui/guis/ScrollingGui", "gui/guis/StorageGui"};

    private GuiCompatibilityPatcher() { }

    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 1) throw new IllegalArgumentException("Expected the class output directory");
        Path output = Paths.get(arguments[0]);
        for (String name : CLASSES) {
            byte[] original;
            try (InputStream stream = GuiCompatibilityPatcher.class.getClassLoader().getResourceAsStream(ORIGINAL + name + ".class")) {
                if (stream == null) throw new IOException("Missing upstream GUI class " + name);
                original = read(stream);
            }
            byte[] patched = patch(ORIGINAL + name, original);
            Path destination = output.resolve(RELOCATED + name + ".class");
            Files.createDirectories(destination.getParent());
            if (!Files.exists(destination) || !Arrays.equals(Files.readAllBytes(destination), patched))
                Files.write(destination, patched);
        }
    }

    static byte[] patch(String expectedName, byte[] original) {
        ClassReader reader = new ClassReader(original);
        String owner = reader.getClassName();
        if (!owner.equals(expectedName) && !owner.equals(relocate(expectedName)))
            throw new IllegalArgumentException("Expected " + expectedName + " but found " + owner);
        Map<String, String> required = new LinkedHashMap<>();
        required.put("open(" + HUMAN + ")V", "(" + HUMAN + ")V");
        if (expectedName.endsWith("/BaseGui")) {
            required.put("close(" + HUMAN + ")V", "(" + HUMAN + ")V");
            required.put("close(" + HUMAN + "Z)V", "(" + HUMAN + "Z)V");
        } else if (!expectedName.endsWith("/StorageGui")) {
            required.put("open(" + HUMAN + "I)V", "(" + HUMAN + "I)V");
        }
        Map<String, String[]> targets = new LinkedHashMap<>();
        Map<String, Boolean> aliases = new LinkedHashMap<>();
        ClassWriter writer = new ClassWriter(0);
        Remapper remapper = new Remapper() {
            @Override public String map(String internalName) { return relocate(internalName); }
        };
        reader.accept(new ClassVisitor(Opcodes.ASM9, new ClassRemapper(writer, remapper)) {
            @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                String key = name + descriptor;
                if (required.containsKey(key)) {
                    if ((access & Opcodes.ACC_PUBLIC) == 0 || (access & (Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT)) != 0)
                        throw new IllegalStateException("Upstream GUI method cannot be forwarded: " + owner + "." + key);
                    targets.put(key, exceptions);
                }
                if (descriptor.contains(PLAYER)) aliases.put(key, true);
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }

            @Override public void visitEnd() {
                for (Map.Entry<String, String> entry : required.entrySet()) {
                    String targetKey = entry.getKey();
                    if (!targets.containsKey(targetKey))
                        throw new IllegalStateException("Upstream GUI API changed; missing " + owner + "." + targetKey);
                    String descriptor = entry.getValue();
                    String name = targetKey.substring(0, targetKey.indexOf('('));
                    String legacyDescriptor = descriptor.replace(HUMAN, PLAYER);
                    if (aliases.containsKey(name + legacyDescriptor)) continue;
                    MethodVisitor method = super.visitMethod(Opcodes.ACC_PUBLIC, name, legacyDescriptor, null, targets.get(targetKey));
                    method.visitCode();
                    method.visitVarInsn(Opcodes.ALOAD, 0);
                    method.visitVarInsn(Opcodes.ALOAD, 1);
                    boolean secondArgument = descriptor.contains(";I)") || descriptor.contains(";Z)");
                    if (secondArgument) method.visitVarInsn(Opcodes.ILOAD, 2);
                    // Virtual dispatch preserves PaginatedGui/ScrollingGui/StorageGui overrides.
                    method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, descriptor, false);
                    method.visitInsn(Opcodes.RETURN);
                    method.visitMaxs(secondArgument ? 3 : 2, secondArgument ? 3 : 2);
                    method.visitEnd();
                }
                super.visitEnd();
            }
        }, 0);
        return writer.toByteArray();
    }

    static String relocate(String name) {
        if (name.startsWith(ORIGINAL)) return RELOCATED + name.substring(ORIGINAL.length());
        if (name.startsWith("net/kyori/")) return "me/mraxetv/beastlib/lib/kyori/" + name.substring("net/kyori/".length());
        return name;
    }

    private static byte[] read(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        return output.toByteArray();
    }
}
