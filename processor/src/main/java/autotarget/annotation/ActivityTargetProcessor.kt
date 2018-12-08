package autotarget.annotation

import autotarget.MainProcessor
import autotarget.ProcessorUtil.classActivityTarget
import autotarget.ProcessorUtil.classArrayList
import autotarget.ProcessorUtil.classList
import autotarget.ProcessorUtil.classNonNull
import autotarget.ProcessorUtil.classParameterProvider
import autotarget.ProcessorUtil.populateParamListBody
import com.squareup.javapoet.*
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class ActivityTargetProcessor {

    private val listOfParameterProvider = ParameterizedTypeName.get(classList, classParameterProvider)
    private val arrayListOfParameterProvider = ParameterizedTypeName.get(classArrayList, classParameterProvider)

    private val activitiesWithPackage: HashMap<String, String> = HashMap()
    private var targetParameterMap: HashMap<String, Element>? = null

    fun process(mainProcessor: MainProcessor, roundEnv: RoundEnvironment) {
        targetParameterMap = mainProcessor.targetParameterMap

        val fileBuilder = TypeSpec.classBuilder("ActivityTargets")
                .addModifiers(Modifier.PUBLIC)

        preparePackageMap(mainProcessor, roundEnv)
        createMethods(fileBuilder)

        val file = fileBuilder.build()
        JavaFile.builder("autotarget.generated", file)
                .build()
                .writeTo(mainProcessor.filer)
    }

    private fun preparePackageMap(mainProcessor: MainProcessor, roundEnv: RoundEnvironment) {
        for (it in roundEnv.getElementsAnnotatedWith(ActivityTarget::class.java)) {
            if (!it.kind.isClass) {
                mainProcessor.messager.printMessage(Diagnostic.Kind.ERROR,
                        "Can only be applied to a class. Error for ${it.simpleName}")
                continue
            }

            val typeElement = it as TypeElement
            activitiesWithPackage[typeElement.simpleName.toString()] =
                    mainProcessor.elements.getPackageOf(typeElement).qualifiedName.toString()
        }
    }

    private fun createMethods(fileBuilder: TypeSpec.Builder) {
        activitiesWithPackage.forEach { activityName, packageName ->
            val parameterMap = HashMap<String, ArrayList<TargetParameterItem>>()

            val activityClass = ClassName.get(packageName, activityName)
            val targetParameter = targetParameterMap?.get(activityName)?.getAnnotation(TargetParameter::class.java)
            targetParameter?.value?.forEach {
                it.group.forEach { group ->
                    val list = parameterMap[group] ?: ArrayList()
                    list.add(it)
                    parameterMap[group] = list
                }
            }

            val forceEmptyTargetMethod = targetParameter?.forceEmptyTargetMethod ?: false
            if(forceEmptyTargetMethod || parameterMap.isEmpty()) createDefaultTargetMethod(activityClass, activityName, fileBuilder)

            parameterMap.keys.forEach {
                val parameterItems = parameterMap[it]
                if(parameterItems?.isNotEmpty() == true) {
                    val methodBuilderWithOptionals = MethodSpec.methodBuilder("show${activityName}For${it.capitalize()}")
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .addAnnotation(classNonNull)
                            .returns(classActivityTarget)
                            .addStatement("$listOfParameterProvider parameterList = new $classArrayList<>()")

                    populateParamListBody(parameterItems, methodBuilderWithOptionals)
                    methodBuilderWithOptionals.addStatement("return new $classActivityTarget($activityClass.class, parameterList)")
                    fileBuilder.addMethod(methodBuilderWithOptionals.build())
                }
            }
        }
    }

    private fun createDefaultTargetMethod(activityClass: ClassName, activityName: String, fileBuilder: TypeSpec.Builder) {
        val methodBuilderBase = MethodSpec.methodBuilder("show$activityName")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addAnnotation(classNonNull)
                .returns(classActivityTarget)
                .addStatement("return new $classActivityTarget($activityClass.class, new $arrayListOfParameterProvider())")

        fileBuilder.addMethod(methodBuilderBase.build())
    }
}
