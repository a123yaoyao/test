package com.neo.web;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.neo.annotation.MyColumn;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wh
 * @date 2018年8月22日 下午2:17:28
 *  @Description: 生成API文档
 */
@RestController
@EnableAutoConfiguration
@RequestMapping(value="/api",name="测试生成接口文档")
public class APIDocController {

    List<String> classPaths = new ArrayList<String>();
    /**
     * 获取表名和注释以及表的字段信息
     */
    @RequestMapping(value="/list",method=RequestMethod.GET,name="获取表名和注释以及表的字段信息")
    public Map<String,Object> list(@RequestParam Map<String,Object> param, Integer pageSize) {
        try {
            //接口文件包的根路径
            String basePack = "com.neo.web";
            String classpath = APIDocController.class.getResource("/").getPath();
            basePack = basePack.replace(".", File.separator);
            String searchPath = classpath + basePack;
            List<String> allClasses = new ArrayList<String>();
            doPath(new File(searchPath));
            for (String s : classPaths) {
                s = s.replace(classpath.replace("/","\\").replaceFirst("\\\\",""),"")
                        .replace("\\",".").replace(".class","");
                allClasses.add(s);
            }
            Map<String,Object> data = new HashMap<String,Object>();
            data.put("apiList", getApiInfo(allClasses));
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取指定路径下的所有类名
     * @param file
     */
    private void doPath(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f1 : files) {
                doPath(f1);
            }
        } else {
            if (file.getName().endsWith(".class")) {
                classPaths.add(file.getPath());
            }
        }
    }

    /**获取类的注释、路径和所有方法的注释、路径、输入、输出信息
     * @param clsList
     * @return
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    public  List<Map<String,Object>> getApiInfo(List<String> clsList) throws
            ClassNotFoundException{
        List<Map<String,Object>> clsInfo = new ArrayList<Map<String,Object>>();
        DefaultParameterNameDiscoverer discover = new DefaultParameterNameDiscoverer();
        if (clsList != null && clsList.size() > 0) {
            for (String clsName : clsList) {
                Class cls =  Class.forName(clsName);
                Map<String,Object> clsMap = new HashMap<String,Object>();
                RequestMapping clsMapp = (RequestMapping)
                        cls.getAnnotation(RequestMapping.class);
                if (clsMapp == null) {
                    continue;
                }else {
                    clsMap.put("clsUrl", clsMapp.value());
                    clsMap.put("clsDesc", clsMapp.name());
                }
                clsMap.put("clsName", clsName);
                List<Map<String,Object>> methodList = new ArrayList<Map<String,Object>>();
                Method[] methods = cls.getDeclaredMethods();
                if (methods != null && methods.length > 0) {
                    for (Method method : methods) {
                        Map<String,Object> methodMap = new HashMap<String,Object>();
                        RequestMapping requestMapp = (RequestMapping)
                                method.getAnnotation(RequestMapping.class);
                        if (requestMapp == null) {
                            continue;
                        }else {
                            methodMap.put("requestUrl", requestMapp.value());
                            methodMap.put("requestType", requestMapp.method());
                            String[] parameterNames = discover.getParameterNames(method);
                            List<Map<String,Object>> paramList = new
                                    ArrayList<Map<String,Object>>();
                            Parameter[] params = method.getParameters();
                            int i=0;
                            for(Parameter param : params) {
                                Map<String,Object> paramMap = new HashMap<String,Object>();
                                /******
                                 * 版本JDK1.8
                                 * store information about method parameters已勾选
                                 * maven编译插件已经添加 -parameters参数
                                 * param.getName()返回的名称依然是arg0,arg1...
                                 paramMap.put("paramName", param.getName());
                                 */
                                paramMap.put("paramName", parameterNames[i]);
                                paramMap.put("paramType",getType(param.getType().getName()));
                                paramList.add(paramMap);
                                i++;
                            }
                            methodMap.put("methodParams", paramList);
                            methodMap.put("returnType",
                                    getType(method.getReturnType().getName()));
                            methodMap.put("methodDesc", requestMapp.name());
                        }
                        methodList.add(methodMap);
                    }
                }
                clsMap.put("methodList", methodList);
                clsInfo.add(clsMap);
            }
        }
        return clsInfo;
    }

    /**处理函数返回类型
     * @param className
     * @return
     * @throws ClassNotFoundException
     */
    private Object getType(String className) throws ClassNotFoundException {
        if(isBasicClass(className)) {
            return getShortClassName(className);
        }
        try {
            return getClassField(Class.forName(className));
        }catch(Exception e) {
            return null;
        }
    }


    /**获取类名的最后一部分
     * @param className
     * @return
     */
    private String getShortClassName(String className) {
        if(className == null
                || "".equals(className)
                ||className.indexOf(".")<0) {
            return className;
        }
        return className.substring(className.lastIndexOf(".")+1);
    }

    /**判断是否是基本类
     * @param c
     * @return
     */
    private boolean isBasicClass(String className) {
        String[] arr =  {"Byte","Short","Integer","Long","Float","Double",
                "Character","Boolean",
                "byte","short","int","long",
                "float","double","char",
                "boolean","String","Map"};
        List<String> packList = Arrays.asList(arr);
        try {
            if(packList.contains(className)){
                return true;
            }else {
                Class c = Class.forName(className);
                if(c == null) {
                    return true;
                }
                if(c.isPrimitive()) {
                    return true;
                }
                if(className.indexOf(".")>-1) {
                    className = className.substring(className.lastIndexOf(".")+1);
                }
                if(packList.contains(className)){
                    return true;
                }else {
                    return false;
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    /**获取类的属性信息
     * @return
     */
    private Map<String,Object> getClassField(Class c){
        Map<String,Object> result = new HashMap<String,Object>();
        try {
            if(c == null) {
                return null;
            }
            Field[] fs = c.getDeclaredFields();
            for(Field f : fs) {
                /***
                 * 此处仅简单返回pojo类的属性名称和类型
                 * 可以通过自定义注解，添加诸如中文名，长度，是否必须，备注信息，
                 * 哪些字段不需要等
                 * 对于类型又是pojo类型的没有做递归处理
                 */
                String fType = f.getType().getName();
                if(fType.indexOf(".")>-1) {
                    fType = fType.substring(fType.lastIndexOf(".")+1);
                }
                result.put(f.getName(), fType);
                /***
                 * 如果要获取字段的自定义注解信息，调用下边这个方法
                 * 这样做,需要修改原有POJO的属性定义部分代码
                 getColumnAnnoInfo(result,f);
                 ***/
            }
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }


    /**获取字段的注解信息
     * @param fieldMap 字段信息map
     * @param f        目标字段
     * @return
     */
    private Map<String,Object> getColumnAnnoInfo(Map<String,Object> fieldMap, Field f) {

        try {
            MyColumn myc = (MyColumn) f.getAnnotation(MyColumn.class);
            if(myc != null) {
                fieldMap.put("cname",myc.name());
                fieldMap.put("length",myc.length());
                fieldMap.put("isRequire",myc.isRequire());
                fieldMap.put("memo",myc.memo());
            }
        }catch(Exception e) {
            e.printStackTrace();
            return fieldMap;
        }
        return fieldMap;
    }
}

