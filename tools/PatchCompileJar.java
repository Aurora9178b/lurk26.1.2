import java.io.*;
import java.nio.file.*;
import java.util.jar.*;
import org.objectweb.asm.*;

public class PatchCompileJar {
    static int publicAccess(int access) {
        return (access | Opcodes.ACC_PUBLIC) & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED);
    }
    public static void main(String[] args) throws Exception {
        Path input = Paths.get(args[0]);
        Path output = Paths.get(args[1]);
        Files.createDirectories(output.getParent());
        try (JarFile jar = new JarFile(input.toFile()); JarOutputStream out = new JarOutputStream(Files.newOutputStream(output))) {
            jar.stream().forEach(entry -> {
                try {
                    if (entry.isDirectory()) return;
                    byte[] bytes = jar.getInputStream(entry).readAllBytes();
                    if (entry.getName().endsWith(".class") && (entry.getName().startsWith("net/minecraft/") || entry.getName().startsWith("com/mojang/blaze3d/"))) {
                        ClassReader cr = new ClassReader(bytes);
                        ClassWriter cw = new ClassWriter(0);
                        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
                            @Override public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                                super.visit(version, publicAccess(access), name, signature, superName, interfaces);
                            }
                            @Override public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                                return super.visitField(publicAccess(access), name, descriptor, signature, value);
                            }
                            @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                                return super.visitMethod(publicAccess(access), name, descriptor, signature, exceptions);
                            }
                            @Override public void visitInnerClass(String name, String outerName, String innerName, int access) {
                                super.visitInnerClass(name, outerName, innerName, publicAccess(access));
                            }
                        }, 0);
                        bytes = cw.toByteArray();
                    }
                    JarEntry outEntry = new JarEntry(entry.getName());
                    outEntry.setTime(entry.getTime());
                    out.putNextEntry(outEntry);
                    out.write(bytes);
                    out.closeEntry();
                } catch (Exception e) { throw new RuntimeException(e); }
            });
        }
    }
}
