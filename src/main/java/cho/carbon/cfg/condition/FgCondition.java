package cho.carbon.cfg.condition;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import cho.carbon.cfg.annotation.FunctionGroup;

public class FgCondition implements Condition{


	/**
	 * context:  判断条件能使用的上下文环境
	 * metadata： 当前标注了这个注解的注释信息
	 */
	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		// 获取到bean定义的注册类
		// 能注册一个bean, 移除， 查看
		BeanDefinitionRegistry registry = context.getRegistry();
		//获取到ioc 使用的beanFactory 
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		// 获取所有 标记了FunctionGroup  注解的bean name
		String[] beanNamesForAnnotation = beanFactory.getBeanNamesForAnnotation(FunctionGroup.class);
		for (String name : beanNamesForAnnotation) {
			// 获取当前bean
			Class<? extends Object> clazz =  beanFactory.getBean(name).getClass();
			//建立controller clazz 对象
			Class buildController = buildController(clazz);
			if (buildController != null) {
				BeanDefinition controllerBean = new RootBeanDefinition(buildController);
				registry.registerBeanDefinition(buildController.getName(), controllerBean);
			}
		}
		
		return true;
	}
	
	// 动态生成restController 接口
	private  Class buildController(Class<? extends Object> clazz)  {
		// 获取类的简单名称
		String clazzName = clazz.getSimpleName();
		String clazzNameLower = clazzName.toLowerCase();
		// 构件controller 名称
		String controllerName = clazzName + "Controller";
		String rt = "\r\n";
		StringBuffer sb = new StringBuffer();
		
		String packageName = "cho.carbon.cfg.controller";
		sb.append("package  "+packageName+";" +rt)
		.append("import org.springframework.web.bind.annotation.RequestMapping;" +rt)
		.append("import org.springframework.web.bind.annotation.RestController;" +rt)
		.append("import org.springframework.beans.factory.annotation.Autowired;" +rt)
		.append("import org.springframework.web.bind.annotation.RequestMethod;" +rt)
		.append("import org.springframework.web.bind.annotation.RequestParam;" +rt)
		.append("import java.lang.String;" +rt)
		.append("import java.lang.Integer;" +rt)
		
		.append("@RestController" +rt)
		.append("public class "+controllerName+" {" +rt)
		.append("@Autowired"+rt)
		.append(clazz.getName() + " " +clazzNameLower +";"+rt);
		// 获取当前bean 所有的接口
		Class<?>[] interfaces = clazz.getInterfaces();
		System.out.println(" 实现的接口： " + Arrays.toString(interfaces));
		// 遍历所有接口
		for (Class<?> inter : interfaces) {
			// 遍历所有接口方法
			for (Method method : inter.getMethods()) {
				String methodName = method.getName();
				String methodParam1 = "";
				String methodParam2 = "";
				Integer count = 0;
				// 获取方法参数
				Class<?>[] parameterTypes = method.getParameterTypes();
				for (Class<?> class1 : parameterTypes) {
					String parameterType = class1.getName();
					
					String arg = "arg" + count;
					// 构件方法
					methodParam1 += "@RequestParam(value=\""+arg+"\")" + parameterType +" "+arg +",";
					methodParam2 +=arg + ",";
					count++;
				}
				if (!methodParam1.isEmpty()) {
					methodParam1 = methodParam1.substring(0, methodParam1.length()-1).trim();
				}
				if (!methodParam2.isEmpty()) {
					methodParam2 = methodParam2.substring(0, methodParam2.length()-1).trim();
				}
				
				// 获取返回值类型
				String returnType = method.getReturnType().getName();
				// 根据接口方法， 构件contrler方法
				sb.append("@RequestMapping(value=\"/"+clazzNameLower+"/" +methodName+ "\", method = RequestMethod.GET)" +rt)
				.append("public "+returnType+" "+methodName+"("+methodParam1+") {" +rt)
				.append(" return  "+clazzNameLower+"." + methodName +"("+methodParam2+");" +rt)
				.append("}" +rt);
			}
		}
		// 类结束标志
		sb.append("}" +rt);
		
		String filePath = System.getProperty("user.dir")+ "/target/classes/cho/carbon/cfg/controller/"+controllerName;
		String javaFileName = filePath + ".java";
		File file = new File(javaFileName);
		
		try {
			FileUtils.writeStringToFile(file, sb.toString());
			// 拿到编译器
			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			// 文件管理者
			StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
			// 获取文件
			Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(file);
			//编译任务
			CompilationTask task = compiler.getTask(null, fileManager, null, null, null, compilationUnits);
			task.call();
			fileManager.close();
			// 删除java源文件
			file.delete();
			
			ClassLoader classLoader = ClassLoader.getSystemClassLoader();
			Class<?> loadClass = classLoader.loadClass(packageName + "."+controllerName);
			// java .class 文件没有删除。  测试后确定是否要删除
			String clazzFileName = filePath + ".class";
			File clazzFile = new File(clazzFileName);
			clazzFile.delete();
			
			return loadClass;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
		
	}
	

}
