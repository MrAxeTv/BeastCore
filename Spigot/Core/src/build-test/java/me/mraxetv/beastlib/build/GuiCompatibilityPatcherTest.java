package me.mraxetv.beastlib.build;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Build-time executable regression tests; no Bukkit server or runtime ASM dependency is needed. */
public final class GuiCompatibilityPatcherTest {
    private static final String BASE = GuiCompatibilityPatcher.ORIGINAL + "gui/guis/BaseGui";
    private static final String PAGINATED = GuiCompatibilityPatcher.ORIGINAL + "gui/guis/PaginatedGui";
    private static final String SCROLLING = GuiCompatibilityPatcher.ORIGINAL + "gui/guis/ScrollingGui";
    private static final String STORAGE = GuiCompatibilityPatcher.ORIGINAL + "gui/guis/StorageGui";
    private static final String GUI = GuiCompatibilityPatcher.ORIGINAL + "gui/guis/Gui";
    private static final String SUBCLASS = GuiCompatibilityPatcher.ORIGINAL + "gui/guis/LegacyDispatchFixture";
    private static final String LEGACY_CALLER = "me/mraxetv/beastlib/build/LegacyGuiCallerFixture";

    private GuiCompatibilityPatcherTest() { }

    public static void main(String[] arguments) throws Exception {
        Map<String, byte[]> definitions = new HashMap<>();
        addPatched(definitions, BASE, fixture(BASE, "java/lang/Object", true, 10));
        addPatched(definitions, PAGINATED, fixture(PAGINATED, BASE, false, 40));
        addPatched(definitions, SCROLLING, fixture(SCROLLING, PAGINATED, false, 50));
        addPatched(definitions, STORAGE, storage());
        definitions.put(binary(GuiCompatibilityPatcher.relocate(GUI)), subclass(GuiCompatibilityPatcher.relocate(GUI), GuiCompatibilityPatcher.relocate(BASE), false));
        definitions.put(binary(GuiCompatibilityPatcher.relocate(SUBCLASS)), subclass(GuiCompatibilityPatcher.relocate(SUBCLASS), GuiCompatibilityPatcher.relocate(BASE), true));
        definitions.put(binary(LEGACY_CALLER), legacyCaller());
        ClassLoader loader = new ClassLoader(GuiCompatibilityPatcherTest.class.getClassLoader()) {
            @Override protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!definitions.containsKey(name)) return super.loadClass(name, resolve);
                Class<?> type = findLoadedClass(name);
                if (type == null) type = findClass(name);
                if (resolve) resolveClass(type);
                return type;
            }
            @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] bytes = definitions.get(name);
                if (bytes == null) throw new ClassNotFoundException(name);
                return defineClass(name, bytes, 0, bytes.length);
            }
        };
        Class<?> player = Class.forName("org.bukkit.entity.Player", false, loader);
        Class<?> human = Class.forName("org.bukkit.entity.HumanEntity", false, loader);
        Class<?> base = loader.loadClass(binary(GuiCompatibilityPatcher.relocate(BASE)));
        Object baseInstance = base.getDeclaredConstructor().newInstance();
        base.getDeclaredMethod("open", human);
        base.getDeclaredMethod("open", player).invoke(baseInstance, new Object[]{null});
        marker(baseInstance, 10, "legacy open(Player)");
        base.getDeclaredMethod("close", player).invoke(baseInstance, new Object[]{null});
        marker(baseInstance, 20, "legacy close(Player)");
        base.getDeclaredMethod("close", player, boolean.class).invoke(baseInstance, null, true);
        marker(baseInstance, 31, "legacy close(Player, boolean)");
        base.getDeclaredMethod("close", player, boolean.class).invoke(baseInstance, null, false);
        marker(baseInstance, 30, "close flag forwarding");

        Object overridden = loader.loadClass(binary(GuiCompatibilityPatcher.relocate(SUBCLASS))).getDeclaredConstructor().newInstance();
        base.getDeclaredMethod("open", player).invoke(overridden, new Object[]{null});
        marker(overridden, 99, "inherited legacy open dispatches to overridden open(HumanEntity)");

        Object paginated = loader.loadClass(binary(GuiCompatibilityPatcher.relocate(PAGINATED))).getDeclaredConstructor().newInstance();
        paginated.getClass().getDeclaredMethod("open", player, int.class).invoke(paginated, null, 7);
        marker(paginated, 7, "paginated page argument");
        Object scrolling = loader.loadClass(binary(GuiCompatibilityPatcher.relocate(SCROLLING))).getDeclaredConstructor().newInstance();
        scrolling.getClass().getDeclaredMethod("open", player, int.class).invoke(scrolling, null, 8);
        marker(scrolling, 8, "scrolling page argument");
        Object storage = loader.loadClass(binary(GuiCompatibilityPatcher.relocate(STORAGE))).getDeclaredConstructor().newInstance();
        storage.getClass().getDeclaredMethod("open", player).invoke(storage, new Object[]{null});
        marker(storage, 60, "storage declared legacy overload");
        base.getDeclaredMethod("open", player).invoke(storage, new Object[]{null});
        marker(storage, 60, "base legacy overload preserves StorageGui virtual dispatch");

        Object gui = loader.loadClass(binary(GuiCompatibilityPatcher.relocate(GUI))).getDeclaredConstructor().newInstance();
        loader.loadClass(binary(LEGACY_CALLER)).getMethod("open", gui.getClass(), player).invoke(null, gui, null);
        marker(gui, 10, "compiled Gui.open(Player) constant-pool descriptor resolves through BaseGui");

        try {
            GuiCompatibilityPatcher.patch(BASE, fixture(BASE, "java/lang/Object", false, 10));
            throw new AssertionError("An incompatible upstream method set must fail the build");
        } catch (IllegalStateException expected) {
            if (!expected.getMessage().contains("missing")) throw expected;
        }
        System.out.println("GUI compatibility checks passed: legacy descriptors, virtual dispatch, arguments, idempotency, upstream validation.");
    }

    private static void addPatched(Map<String, byte[]> definitions, String name, byte[] bytes) {
        byte[] patched = GuiCompatibilityPatcher.patch(name, bytes);
        byte[] secondPass = GuiCompatibilityPatcher.patch(name, patched);
        Set<String> first = methods(patched);
        if (!first.equals(methods(secondPass))) throw new AssertionError("Patch must be idempotent for " + name);
        definitions.put(binary(GuiCompatibilityPatcher.relocate(name)), patched);
    }

    private static Set<String> methods(byte[] bytes) {
        Set<String> result = new HashSet<>();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if (!result.add(name + descriptor)) throw new AssertionError("Duplicate GUI method " + name + descriptor);
                return null;
            }
        }, ClassReader.SKIP_CODE);
        return result;
    }

    private static byte[] fixture(String name, String parent, boolean base, int openValue) {
        ClassWriter writer = start(name, parent);
        if (name.equals(BASE)) writer.visitField(Opcodes.ACC_PUBLIC, "marker", "I", null, null).visitEnd();
        resultMethod(writer, name, "open", "(" + GuiCompatibilityPatcher.HUMAN + ")V", openValue, false);
        if (base) {
            resultMethod(writer, name, "close", "(" + GuiCompatibilityPatcher.HUMAN + ")V", 20, false);
            resultMethod(writer, name, "close", "(" + GuiCompatibilityPatcher.HUMAN + "Z)V", 30, true);
        } else resultMethod(writer, name, "open", "(" + GuiCompatibilityPatcher.HUMAN + "I)V", 0, true);
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] subclass(String name, String parent, boolean override) {
        ClassWriter writer = start(name, parent);
        if (override) resultMethod(writer, name, "open", "(" + GuiCompatibilityPatcher.HUMAN + ")V", 99, false);
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] storage() {
        ClassWriter writer = start(STORAGE, BASE);
        resultMethod(writer, STORAGE, "open", "(" + GuiCompatibilityPatcher.HUMAN + ")V", 60, false);
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static ClassWriter start(String name, String parent) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, name, null, parent, null);
        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, parent, "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(1, 1);
        constructor.visitEnd();
        return writer;
    }

    private static void resultMethod(ClassWriter writer, String owner, String name, String descriptor, int constant, boolean argument) {
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, name, descriptor, null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitIntInsn(Opcodes.BIPUSH, constant);
        if (argument) {
            method.visitVarInsn(Opcodes.ILOAD, 2);
            method.visitInsn(Opcodes.IADD);
        }
        String fieldOwner = owner.startsWith(GuiCompatibilityPatcher.RELOCATED) ? GuiCompatibilityPatcher.relocate(BASE) : BASE;
        method.visitFieldInsn(Opcodes.PUTFIELD, fieldOwner, "marker", "I");
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(3, argument ? 3 : 2);
        method.visitEnd();
    }

    private static byte[] legacyCaller() {
        ClassWriter writer = start(LEGACY_CALLER, "java/lang/Object");
        String gui = GuiCompatibilityPatcher.relocate(GUI);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "open", "(L" + gui + ";" + GuiCompatibilityPatcher.PLAYER + ")V", null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitVarInsn(Opcodes.ALOAD, 1);
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, gui, "open", "(" + GuiCompatibilityPatcher.PLAYER + ")V", false);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(2, 2);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void marker(Object gui, int expected, String description) throws Exception {
        int actual = gui.getClass().getField("marker").getInt(gui);
        if (actual != expected) throw new AssertionError(description + ": expected " + expected + " but got " + actual);
    }

    private static String binary(String internal) { return internal.replace('/', '.'); }
}
