package cc.uraniummc.eventexecutor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.val;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import cpw.mods.fml.relauncher.ReflectionHelper;

import static org.objectweb.asm.Opcodes.*;

public class ASMEventExecutorGenerate{

    public static String getUniqueName(Method pMethod){
        return String.format("%s_%d_%s_%s",pMethod.getDeclaringClass().getName(),IDs.incrementAndGet(),
                pMethod.getName(),
                pMethod.getParameterTypes()[0].getSimpleName());
    }

    public static void removeListener(Plugin pPlugin){
        cache.remove(pPlugin);
    }

    private static final String EXECUTE_DESC=Type.getInternalName(EventExecutor.class);
    private static final String EXECUTE_METHOD_DESC="(Lorg/bukkit/event/Listener;Lorg/bukkit/event/Event;)V";

    private static AtomicInteger IDs=new AtomicInteger();
    private static final HashMap<Plugin,HashMap<Method,Class<?>>> cache=new HashMap();

    /** defineClass(String, byte[], int off, int len) */
    private static final Method method_ClassLoader_defineClass=ReflectionHelper.findMethod(ClassLoader.class,
            null,new String[]{"defineClass"},
            String.class,byte[].class,int.class,int.class);

    static{
        method_ClassLoader_defineClass.setAccessible(true);
    }

    /**
     * 创建由ASM构造的执行类实例
     * 
     * @param pPlugin
     *            插件
     * @param pTarget
     *            执行方法的实例
     * @param pMethod
     *            执行的方法
     * @return 执行类实例
     */
    public static EventExecutorImp createWrapperInstance(Plugin pPlugin,Object pTarget,Method pMethod){
        try{
            Class<?> tWrapper=createWrapper(pPlugin,pMethod);
            if(tWrapper!=null){
                return (EventExecutorImp)tWrapper.getConstructor().newInstance();
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 创建由ASM构造的执行类,会使用缓存
     * 
     * @param pPlugin
     *            插件,用于查询缓存与加载生成的类
     * @param pMethod
     *            执行的方法
     * @return 生成的执行类
     */
    public static Class<?> createWrapper(Plugin pPlugin,Method pMethod){
        HashMap<Method,Class<?>> tMap=cache.get(pPlugin);
        if(tMap!=null&&tMap.containsKey(pMethod)){
            return tMap.get(pMethod);
        }

        Class<?> tOwner=pMethod.getDeclaringClass();
        if(!canAccess(tOwner,pMethod)) return null;
        String tName=getUniqueName(pMethod);
        String tDesc=tName.replace('.','/');

        ClassWriter tCW=new ClassWriter(3);
        tCW.visit(V1_6,ACC_PUBLIC|ACC_SUPER,tDesc,null,Type.getInternalName(EventExecutorImp.class),new String[]{EXECUTE_DESC});
        tCW.visitSource("Uranium_ASMEventExecutorGenerate.java",null);

        GeneratorAdapter tMethodGenerator=new GeneratorAdapter(tCW.visitMethod(ACC_PUBLIC,"<init>","()V",null,null),ACC_PUBLIC,"<init>","()V");
        tMethodGenerator.loadThis();
        tMethodGenerator.visitMethodInsn(INVOKESPECIAL,Type.getInternalName(EventExecutorImp.class),"<init>","()V",false);
        tMethodGenerator.returnValue();
        tMethodGenerator.endMethod();

        tMethodGenerator=new GeneratorAdapter(tCW.visitMethod(1,"invoke",EXECUTE_METHOD_DESC,null,null),1,"execute",EXECUTE_METHOD_DESC);
        val nonStaticMethod=(pMethod.getModifiers()&Modifier.STATIC)==0;
        if(nonStaticMethod){
            tMethodGenerator.loadArg(0);
            tMethodGenerator.checkCast(Type.getType(tOwner));
        }
        tMethodGenerator.loadArg(1);
        tMethodGenerator.checkCast(Type.getType(pMethod.getParameterTypes()[0]));
        tMethodGenerator.visitMethodInsn(
                nonStaticMethod?(tOwner.isInterface()?INVOKEINTERFACE:INVOKEVIRTUAL):INVOKESTATIC,
                Type.getInternalName(tOwner),pMethod.getName(),Type.getMethodDescriptor(pMethod),tOwner.isInterface());
        if(pMethod.getReturnType()!=Void.TYPE){
            tMethodGenerator.pop();
        }
        tMethodGenerator.returnValue();
        tMethodGenerator.endMethod();

        tCW.visitEnd();

        Class<?> tClazz=defineClass(tName,tCW.toByteArray(),tOwner.getClassLoader());
        if(tClazz==null) return null;

        if(tMap==null){
            tMap=new HashMap();
            cache.put(pPlugin,tMap);
        }

        tMap.put(pMethod,tClazz);
        return tClazz;
    }

    private static boolean canAccess(Class<?> pTarget,Method pMethod){
        return Modifier.isPublic(pTarget.getModifiers())&&Modifier.isPublic(pMethod.getModifiers());
    }

    public static Class<?> defineClass(String pName,byte[] pData,ClassLoader pLoader){
        try{
            return (Class<?>)method_ClassLoader_defineClass.invoke(pLoader,pName,pData,0,pData.length);
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
