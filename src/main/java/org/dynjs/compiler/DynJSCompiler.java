package org.dynjs.compiler;

import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.JDKVersion;
import me.qmx.jitescript.JiteClass;
import org.dynjs.api.Function;
import org.dynjs.api.Scope;
import org.dynjs.parser.Statement;
import org.dynjs.runtime.DynAtom;
import org.dynjs.runtime.DynFunction;
import org.dynjs.runtime.DynThreadContext;
import org.dynjs.runtime.DynamicClassLoader;
import org.dynjs.runtime.Script;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;

import static me.qmx.jitescript.CodeBlock.newCodeBlock;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;

public class DynJSCompiler {

    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final String PACKAGE = "org.dynjs.gen.".replace('.', '/');
    private static final boolean DEBUG = false;

    public Function compile(final DynFunction arg) {
        String className = PACKAGE + "AnonymousDynFunction" + counter.incrementAndGet();
        JiteClass jiteClass = new JiteClass(className, p(DynFunction.class), new String[]{p(Function.class)}) {{
            defineMethod("<init>", ACC_PUBLIC | ACC_VARARGS, sig(void.class, String[].class),
                    newCodeBlock()
                            .aload(0)
                            .aload(1)
                            .invokespecial(p(DynFunction.class), "<init>", sig(void.class, String[].class))
                            .voidreturn()
            );
            defineMethod("call", ACC_PUBLIC | ACC_VARARGS, sig(DynAtom.class, DynThreadContext.class, Scope.class, DynAtom[].class), arg.getCodeBlock());
        }};
        final DynamicClassLoader classLoader = new DynamicClassLoader();
        byte[] bytecode = jiteClass.toBytes(JDKVersion.V1_7);
        Class<?> functionClass = classLoader.define(className.replace('/', '.'), bytecode);
        try {
            Constructor<?> ctor = functionClass.getDeclaredConstructor(String[].class);
            return (Function) ctor.newInstance(new Object[]{arg.getArguments()});
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    public Script compile(final Statement... statements) {
        String className = PACKAGE + "AnonymousDynScript" + counter.incrementAndGet();
        JiteClass jiteClass = new JiteClass(className, p(BaseScript.class), new String[]{p(Script.class)}) {
            {
                defineMethod("<init>", ACC_PUBLIC | ACC_VARARGS, sig(void.class, Statement[].class),
                        newCodeBlock()
                                .aload(0)
                                .aload(1)
                                .invokespecial(p(BaseScript.class), "<init>", sig(void.class, Statement[].class))
                                .voidreturn()
                );
                defineMethod("execute", ACC_PUBLIC | ACC_VARARGS, sig(void.class, DynThreadContext.class, Scope.class), getCodeBlock());
            }

            private CodeBlock getCodeBlock() {
                final CodeBlock block = newCodeBlock();
                for (Statement statement : statements) {
                    block.append(statement.getCodeBlock());
                }
                return block;
            }
        };
        final DynamicClassLoader classLoader = new DynamicClassLoader();
        byte[] bytecode = jiteClass.toBytes(JDKVersion.V1_7);
        Class<?> functionClass = classLoader.define(className.replace('/', '.'), bytecode);
        try {
            Constructor<?> ctor = functionClass.getDeclaredConstructor(Statement[].class);
            return (Script) ctor.newInstance(new Object[]{statements});
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

}
