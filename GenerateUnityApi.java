import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.io.PrintWriter;

public class GenerateUnityApi
{
    static List<ExportFunction> functionsToExport = new ArrayList<ExportFunction>();
    public static void main(String[] argsArray) throws Exception
    {
        Charset charset = Charset.forName("ISO-8859-1");
        Path api_path = Paths.get("android/sources/src/com/unity3d/ads/android/unity3d/UnityAdsUnityEngineWrapper.java");
        Boolean methodsStarted = false;
        Boolean commented = false;
        PrintWriter sourceWriter = new PrintWriter("unity-bridge/UnityAdsAndroidBridgeGenerated.cpp", "UTF-8");
        PrintWriter headerWriter = new PrintWriter("unity-bridge/UnityAdsAndroidBridge.h", "UTF-8");
        sourceWriter.println("//This file is generated by GenerateUnityApi.java. Do not edit.");
        sourceWriter.println("#include \"PlatformDependent/AndroidPlayer/Source/UnityAds/UnityAdsAndroidBridgeJNI.h\"");
        for (String line : Files.readAllLines(api_path, charset))
        {
            if (line.startsWith("//"))
                continue;
            if (line.contains("/*"))
                line = line.substring(0, line.indexOf("/*"));
            if (line.contains("*/"))
                line = line.substring(line.indexOf("*/")+2);
            if (!methodsStarted)
            {
                if (line.contains("{"))
                    methodsStarted = true;
            }
            else 
            {
                if (line.contains("public"))
                {
                    int idx = line.indexOf("public") + 6;
                    int len = line.substring(idx).indexOf("(");
                    String str1 = line.substring(idx, len + 8).trim();
                    String[] parts = str1.split(" ");
                    String paramsStr = line.substring(idx + len);
                    paramsStr = paramsStr.substring(1, paramsStr.indexOf(")")).trim();
                    String[] params = paramsStr.split(",");
                    //if (paramsStr.length() > 0)
                      //  System.out.println("params:" + paramsStr);
                    String output = "static ";
                    if (parts.length == 1)
                    {
                        //output += "void "
                        //System.out.println(parts[0]);
                    }
                    else
                    {
                        ExportFunction ef = new ExportFunction();
                        String type = parts[0].trim();
                        String funcName = parts[1].trim();
                        if (funcName.startsWith("on"))
                            continue;
                        String outType = JavaToCppType(type, false);

                        String outFuncName = Character.toUpperCase(funcName.charAt(0)) + funcName.substring(1);
                        String outParams = "";
                        int overload = 0;
                        for (ExportFunction ef2 : functionsToExport)
                        {
                            if (ef2.funcName.equals(funcName) || (ef2.funcName.contains("___") && ef2.funcName.substring(0, ef2.funcName.indexOf("___")).equals(funcName)))
                            {
                                overload++;
                            }
                        }
                        if (overload > 0)
                            ef.funcName = funcName + "___" + overload;
                        else
                            ef.funcName = funcName;
                        ef.returnType = type;
                        for (int i = 0; i < params.length; i++)
                        {
                            if (params[i].trim().length() == 0)
                                continue;
                            String[] parts2 = params[i].trim().split(" ");
                            String argT = "";
                            int argOffset = 0;
                            if (parts2[0].trim().equals("final"))
                                argOffset = 1;
                            outParams += JavaToCppType(parts2[argOffset], true);
                            outParams += " " + parts2[argOffset + 1];
                            if (i < params.length - 1)
                                outParams += ", ";
                            ef.argTypes.add(parts2[argOffset]);
                            ef.args.add(parts2[argOffset + 1]);
                        }
                        functionsToExport.add(ef);
                        output = "static " + outType + " " + outFuncName + "(" + outParams + ")";
                    }
                    //System.out.println(output + "\n{\n\n}");
                }
            }
        }
        sourceWriter.println("");
        sourceWriter.println("/* FUNCTION POINTERS */");
        for (ExportFunction ef : functionsToExport)
        {
            sourceWriter.println("static jmethodID " + ef.funcName + ";");
        }
        sourceWriter.println("");
        //sourceWriter.println("")
        sourceWriter.println("void RegisterMethods()");
        sourceWriter.println("{");
        sourceWriter.println("\tJAVA_ATTACH_CURRENT_THREAD();");
        sourceWriter.println();
        for (ExportFunction ef : functionsToExport)
        {
            String funcName = ef.funcName;
            if (funcName.contains("___"))
                funcName = funcName.substring(0, funcName.indexOf("___"));
            String paramsStr = "";
            for (int p = 0; p < ef.args.size(); p++)
            {
                paramsStr += JavaTypeAbbreviation(ef.argTypes.get(p));
            }
            sourceWriter.println("\t" + ef.funcName + " = jni_env->GetMethodID(adsWrapperClass, \"" + funcName + "\", \"(" + paramsStr + ")" + JavaTypeAbbreviation(ef.returnType) + "\");");
            sourceWriter.println("\tif (" + ef.funcName + " == 0)");
            sourceWriter.println("\t\tUNITYADS_DEBUG(\"Couldn\'t find function:" + funcName + "\");");
        }
        sourceWriter.println("}");
        sourceWriter.println("");
        headerWriter.println("#pragma once");
        headerWriter.println("#include \"PlatformDependent/AndroidPlayer/Source/EntryPoint.h\"");
        headerWriter.println("#include <string>");
        headerWriter.println("");
        headerWriter.println("#define JAVA_ATTACH_CURRENT_THREAD() \\\n\tDalvikAttachThreadScoped jni_env(__FUNCTION__)");
        headerWriter.println("");
        headerWriter.println("class UnityAdsAndroidBridge");
        headerWriter.println("{");
        headerWriter.println("public:");
        headerWriter.println("\tstatic void InitJNI(JavaVM* vm, JNINativeMethod callbacks[], int callbacksLength);");
        headerWriter.println("\tstatic void ReleaseJNI();");
        headerWriter.println("/* API */");
        for (ExportFunction ef : functionsToExport)
        {
            String funcName = ef.funcName;
            if (funcName.contains("___"))
                funcName = funcName.substring(0, funcName.indexOf("___"));
            funcName = Character.toUpperCase(funcName.charAt(0)) + funcName.substring(1);
            String paramsStr = "";
            for (int p = 0; p < ef.args.size(); p++)
            {
                paramsStr += JavaToCppType(ef.argTypes.get(p), true);
                paramsStr += " " + ef.args.get(p);
                if (p < ef.args.size() - 1)
                    paramsStr += ", ";
            }
            headerWriter.println("\tstatic " + JavaToCppType(ef.returnType, false) + " " + funcName + "(" + paramsStr + ");"); 
            sourceWriter.println(JavaToCppType(ef.returnType, false) + " UnityAdsAndroidBridge::" + funcName + "(" + paramsStr + ")"); 
            sourceWriter.println("{");
            sourceWriter.println("\tJAVA_ATTACH_CURRENT_THREAD();");
            boolean isVoid = ef.returnType.equals("void");
            if (!isVoid)
            {
                sourceWriter.println("\t" + JavaToCppType(ef.returnType, false) + " retval;");
            }
            String paramsStr2 = "adsWrapperObject, " + ef.funcName + "";
            for (int p = 0; p < ef.args.size(); p++)
            {
                if (ef.argTypes.get(p).equals("String"))
                {
                    sourceWriter.println("\tjstring j_" + ef.args.get(p) + " = jni_env->NewStringUTF(" + ef.args.get(p) + ".c_str());");
                    paramsStr2 += ", j_" + ef.args.get(p);
                }
                else
                {
                    paramsStr2 += ", " + ef.args.get(p);
                }
            }
            String exceptionCheck = "\tif (jni_env->ExceptionOccurred())\n\t{\n\t\tjni_env->ExceptionClear();\n\t}";
            if (isVoid)
            {
                sourceWriter.println("\t" + GetJavaInvokeFunction(ef.returnType) + "(" + paramsStr2 + ");");
                sourceWriter.println(exceptionCheck);
            }
            else if (ef.returnType.equals("String")) 
            {
                sourceWriter.println("\tjstring j_str = " + GetJavaInvokeFunction(ef.returnType) + "(" + paramsStr2 + ");");
                sourceWriter.println(exceptionCheck);
                sourceWriter.println("\tif (j_str == 0)");
                sourceWriter.println("\t\tretval = \"\";");
                sourceWriter.println("\telse");
                sourceWriter.println("\t{");
                sourceWriter.println("\t\tconst char* c_str = jni_env->GetStringUTFChars(j_str, 0);");
                sourceWriter.println("\t\tstd::string str = std::string(c_str);");
                sourceWriter.println("\t\tjni_env->ReleaseStringUTFChars(j_str, c_str);");
                sourceWriter.println("\t\tretval = str;");
                sourceWriter.println("\t}");
            }
            else
            {
                sourceWriter.println("\tretval = " + GetJavaInvokeFunction(ef.returnType) + "(" + paramsStr2 + ");");
                sourceWriter.println(exceptionCheck);
            }
            for (int p = 0; p < ef.args.size(); p++)
            {
                if (ef.argTypes.get(p).equals("String"))
                {
                    sourceWriter.println("\tjni_env->DeleteLocalRef(j_" + ef.args.get(p) + ");");
                }
            }
            if (!isVoid)
            {
                sourceWriter.println("\treturn retval;");
            }
            sourceWriter.println("}");
            sourceWriter.println();
        }
        headerWriter.println("};");
        headerWriter.close();
        sourceWriter.close();
    }

    static String GetJavaInvokeFunction(String type)
    {
        switch (type)
        {
            case "void":
                return "jni_env->CallVoidMethod";
            case "boolean":
                return "jni_env->CallBooleanMethod";
            case "int":
                return "jni_env->CallIntMethod";
            case "double":
                return "jni_env->CallDoubleMethod";
            case "String":
                return "(jstring)jni_env->CallObjectMethod";
        }
        return "jni_env->CallObjectMethod";
    }

    static String JavaToCppType(String type, boolean param)
    {
        switch (type)
        {
            case "void":
                return "void";
            case "boolean":
                //return "jboolean";
                return "bool";
            case "int":
                //return "jint";
                return "int";
            case "double":
                //return "jdouble";
                return "double";
            case "String":
                if (param)
                    return "const std::string&";
                return "std::string";
                //return "jstring";
        }   
        return "jobject";
    }

    static String JavaTypeAbbreviation(String type)
    {
        switch (type)
        {
            case "void":
                return "V";
            case "boolean":
                return "Z";
            case "String":
                return "Ljava/lang/String;";
            case "int":
                return "I";
            case "Activity":
                return "Landroid/app/Activity;";
        }

        return "";
    }

    static String CppToJavaType(String type)
    {
        switch (type)
        {
            case "boolean":
                return "bool";
            case "String":
                return "std::string";
        }
        return type;
    }

    static private class ExportFunction
    {
        public String funcName;
        public String returnType;
        public List<String> args = new ArrayList<String>();
        public List<String> argTypes = new ArrayList<String>();
    }
}
