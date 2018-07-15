package com.popoaichuiniu.jacy;

import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.activation.UnsupportedDataTypeException;

import com.popoaichuiniu.util.Util;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;

import soot.JastAddJ.Opt;
import soot.Main;
import soot.PackManager;
import soot.Scene;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.callbacks.AbstractCallbackAnalyzer;
import soot.jimple.infoflow.android.data.parsers.PermissionMethodParser;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.android.source.parsers.xml.XMLSourceSinkParser;
import soot.jimple.infoflow.rifl.RIFLSourceSinkDefinitionProvider;
import soot.jimple.infoflow.source.data.ISourceSinkDefinitionProvider;
import soot.options.Options;

public class MySetupApplication extends SetupApplication {
	
	public void calculateMyEntrypoints(String sourceSinkFile) throws IOException, XmlPullParserException {
		ISourceSinkDefinitionProvider parser = null;// Common interface for all
//													// classes that support
//													// loading source and sink
//													// definitions
//
//		String fileExtension = sourceSinkFile.substring(sourceSinkFile.lastIndexOf("."));
//		fileExtension = fileExtension.toLowerCase();
//
//		try {
//			if (fileExtension.equals(".xml"))
//				parser = XMLSourceSinkParser.fromFile(sourceSinkFile);
//			else if (fileExtension.equals(".txt"))
//				parser = PermissionMethodParser.fromFile(sourceSinkFile);
//			else if (fileExtension.equals(".rifl"))
//				parser = new RIFLSourceSinkDefinitionProvider(sourceSinkFile);
//			else
//				throw new UnsupportedDataTypeException("The Inputfile isn't a .txt or .xml file.");
//
//			
//		} catch (SAXException ex) {
//			throw new IOException("Could not read XML file", ex);
//		}
		
		//calculateSourcesSinksEntrypoints(parser);
		calculateSourcesSinksEntrypoints(parser);
	}

	public MySetupApplication(String androidJar, String apkFileLocation) {
		super(androidJar, apkFileLocation);
		// TODO Auto-generated constructor stub
	}

	private void calculateCallbackMethods(ARSCFileParser resParser, LayoutFileParser lfp) throws IOException {
		AbstractCallbackAnalyzer jimpleClass = null;

		boolean hasChanged = true;
		while (hasChanged) {
			hasChanged = false;

			// Create the new iteration of the main method
			
			initializeSoot();
			//createMainMethod();// 这些callback基于这个方法，如果不调用否则会丢失回调中的方法

//			if (jimpleClass == null) {
//				// Collect the callback interfaces implemented in the app's
//				// source code
//				jimpleClass = callbackClasses == null ? new DefaultCallbackAnalyzer(config, entrypoints, callbackFile)
//						: new DefaultCallbackAnalyzer(config, entrypoints, callbackClasses);
//				jimpleClass.collectCallbackMethods();
//
//				// Find the user-defined sources in the layout XML files. This
//				// only needs to be done once, but is a Soot phase.
//				lfp.parseLayoutFile(apkFileLocation);
//			} else
//				jimpleClass.collectCallbackMethodsIncremental();

			// Run the soot-based operations
			//run pack
			PackManager.v().getPack("wjpp").apply();//Whole-Jimple Pre-processing Pack
			PackManager.v().getPack("cg").apply();
			PackManager.v().getPack("wjtp").apply();

			// Collect the results of the soot-based phases 回调函数的收集
//			for (Entry<String, Set<SootMethodAndClass>> entry : jimpleClass.getCallbackMethods().entrySet()) {
//				Set<SootMethodAndClass> curCallbacks = this.callbackMethods.get(entry.getKey());
//				if (curCallbacks != null) {
//					if (curCallbacks.addAll(entry.getValue()))
//						hasChanged = true;
//				} else {
//					this.callbackMethods.put(entry.getKey(), new HashSet<>(entry.getValue()));
//					hasChanged = true;
//				}
//			}

//			if (entrypoints.addAll(jimpleClass.getDynamicManifestComponents()))
//				hasChanged = true;
		}

		// Collect the XML-based callback methods
		
		//collectXmlBasedCallbackMethods(resParser, lfp, jimpleClass);
	}

	public void calculateSourcesSinksEntrypoints(String sourceSinkFile) throws IOException, XmlPullParserException {
		ISourceSinkDefinitionProvider parser = null;// Common interface for all
													// classes that support
													// loading source and sink
													// definitions

		String fileExtension = sourceSinkFile.substring(sourceSinkFile.lastIndexOf("."));
		fileExtension = fileExtension.toLowerCase();

		try {
			if (fileExtension.equals(".xml"))
				parser = XMLSourceSinkParser.fromFile(sourceSinkFile);
			else if (fileExtension.equals(".txt"))
				parser = PermissionMethodParser.fromFile(sourceSinkFile);
			else if (fileExtension.equals(".rifl"))
				parser = new RIFLSourceSinkDefinitionProvider(sourceSinkFile);
			else
				throw new UnsupportedDataTypeException("The Inputfile isn't a .txt or .xml file.");

			calculateSourcesSinksEntrypoints(parser);
		} catch (SAXException ex) {
			throw new IOException("Could not read XML file", ex);
		}
	}

	public void calculateSourcesSinksEntrypoints(ISourceSinkDefinitionProvider sourcesAndSinks)
			throws IOException, XmlPullParserException {
		// To look for callbacks, we need to start somewhere. We use the Android
		// lifecycle methods for this purpose.
		
		//this.sourceSinkProvider = sourcesAndSinks;
		ProcessManifest processMan = new ProcessManifest(apkFileLocation);// This
																			// class
																			// provides
																			// easy
																			// access
																			// to
																			// all
																			// data
																			// of
																			// an
																			// AppManifest.
		// Nodes and attributes of a parsed manifest can be changed. A new byte
		// compressed manifest considering the changes can be generated.
		this.appPackageName = processMan.getPackageName();
		this.entrypoints = processMan.getEntryPointClasses();// Gets all classes the contain entry points in this
																// applications四大组件

		// Parse the resource file
		
//		long beforeARSC = System.nanoTime();
//		ARSCFileParser resParser = new ARSCFileParser();// Parser for reading out the contents of Android's
//														// resource.arsc file. Structure declarations and comments taken
//														// from the Android source code and ported from C to Java.
//		resParser.parse(apkFileLocation);
//		logger.info("ARSC file parsing took " + (System.nanoTime() - beforeARSC) / 1E9 + " seconds");
//		this.resourcePackages = resParser.getPackages();// Its value is
//														// [soot.jimple.infoflow.android.resources.ARSCFileParser$ResPackage@3e6fa38a]

		// Add the callback methods
		LayoutFileParser lfp = null;// Parser for analyzing the layout XML files inside an android application
		if (config.getEnableCallbacks()) {// Gets whether the taint analysis shall consider callbacks
			if (callbackClasses != null && callbackClasses.isEmpty()) {
				logger.warn("Callback definition file is empty, disabling callbacks");
			} else {
				//lfp = new LayoutFileParser(this.appPackageName, resParser);
				switch (config.getCallbackAnalyzer()) {// Gets the callback analyzer that is being used in preparation
														// for the taint analysis
				case Fast:
					//calculateCallbackMethodsFast(resParser, lfp);
					calculateCallbackMethodsFast(null,null);
					break;
				case Default:
					calculateCallbackMethods(null,null);
					break;
				default:
					throw new RuntimeException("Unknown callback analyzer");
				}

				// Some informational output
				//System.out.println("Found " + lfp.getUserControls() + " layout controls");
			}
		}

		System.out.println("Entry point calculation done.");

		// Clean up everything we no longer need
	//	soot.G.reset();

//		// Create the SourceSinkManager
//		{
//			Set<SootMethodAndClass> callbacks = new HashSet<>();
//			for (Set<SootMethodAndClass> methods : this.callbackMethods.values())
//				callbacks.addAll(methods);
//
//			sourceSinkManager = new AccessPathBasedSourceSinkManager(this.sourceSinkProvider.getSources(),
//					this.sourceSinkProvider.getSinks(), callbacks, config.getLayoutMatchingMode(),
//					lfp == null ? null : lfp.getUserControlsByID());// SourceSinkManager for Android applications. This
//																	// class uses precise access path-based source and
//																	// sink definitions.
//
//			sourceSinkManager.setAppPackageName(this.appPackageName);
//			sourceSinkManager.setResourcePackages(this.resourcePackages);
//			sourceSinkManager.setEnableCallbackSources(this.config.getEnableCallbackSources());
//		}

		entryPointCreator = createEntryPointCreator();
	}


	protected void initializeSoot() {
		soot.G.reset();// 标准的soot操作，清空soot之前所有操作遗留下的缓存值

		Options.v().set_src_prec(Options.src_prec_apk);// 设置优先处理的文件格式

		//Options.v().set_process_dir(Collections.singletonList(defaultAppPath));// 处理文件夹中所有的class
		// singletonList(T) 方法用于返回一个只包含指定对象的不可变列表

		//Options.v().set_android_jars(androidPlatformPath);// 在该路径下找android.jar

		//////////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////////////////////

		Options.v().set_app(false);//app false：整个app都是application class （app开发者代码和第三方库（包括android support 库））







//		Options.v().set_app(true);//
//
//		List<String> excludeList = new LinkedList<String>();
//		excludeList.add("java.*");
//		excludeList.add("sun.misc.*");//app false也是可以设置这个的
//		excludeList.add("android.*");
//		excludeList.add("org.apache.*");
//		excludeList.add("soot.*");
//		excludeList.add("javax.servlet.*");
//		Options.v().set_exclude(excludeList);//这个底下也有，那个sootConfig.setSootOptions(Options.v());
//
//		List<String> includeList = new LinkedList<String>();
//		includeList.add("android.support.*");//不添加的话会有android.support.* 某类的getActiveBody为空的错误，
//		//为什么android.*不会呢？是应为classpath有android.jar吗？测试了，好像也不行。
//		Options.v().set_include(includeList);




		////////////////////////////////////////////////////////////////////////////////不太懂

		List<String> excludeList = new LinkedList<String>();
		excludeList.add("java.*");
		excludeList.add("sun.misc.*");
		excludeList.add("android.*");//
		excludeList.add("org.apache.*");
		excludeList.add("soot.*");
		excludeList.add("javax.servlet.*");
		excludeList.add("org.javatuples.*");
//		Set<String> libraryList=AndroidInfoHelper.getThirdLibraryPackageNameSet();不去除第三方包，因为其可能也有目标API调用，然后其实那些没有目标API的第三方调用不会出现在最后的简约路径图中。
//		for(String packageName:libraryList)
//		{
//			excludeList.add(packageName+".*");
//		}
		Options.v().set_exclude(excludeList);//这样就不会有这些方法的内部的边。这些方法作为library函数



		Options.v().set_no_bodies_for_excluded(false);//去除的类加不加载body 如果不设置为true ，callgraph中没有到android.jar的方法的边。第三方其他库一旦排除，无论这个false还是true都没边（不需要到android。jar的边，直接分析的是方法的内部判断有没有目标api）

//		Hi John,
//
//
//
//		If the package names are freemarker.*, it’s important to put the asterisk into the exclusion option on the Soot command line as well. Otherwise, Soot will assume that there is a single class called “freemarker” which you would like to exclude.
//
//
//
//		If your code under analysis references a class which is not on the Soot classpath and –allow-phantom-refs is used, this class will automatically be considered as a phantom. Nothing will be loaded, because Soot cannot know where to load it from. If you exclude a class, its structure (method signatures, etc.) will be loaded, but it will be marked as a library class. If you specify –no-bodies-for-excluded, Soot will not load any bodies for those classes, even if some code tries to access the body. Therefore, not putting the class on the classpath is the most rigorous option for guaranteeing that it will not be loaded.
//
//
//
//				Your analysis then needs to deal with such cases. If you have a call to a.foo() and the type of the variable “a” refers to a phantom class, there will not be a callgraph edge from the call site to anywhere and thus there will not be an IFDS call edge. There will, however, still be an IFDS call-to-return edge in which you can detect that you are dealing with such an exclusion case and apply e.g., an external library model to make up for the information loss. Depending on your analysis, you might also be able to simply ignore the effects of the method call on your data flow abstraction, that’s something you as the analysis designer need to decide.
//
//
//
//		Best regards,
//
//				Steven

		Options.v().set_allow_phantom_refs(true);//不设置会报错


		Options.v().set_whole_program(true);// 以全局模式运行，这个默认是关闭的，否则不能构建cg(cg是全局的pack)






		//Phantom classes are classes that are neither in the process directory nor on
//		the Soot classpath, but that are referenced by some class / method body that
//		Soot loads. If phantom classes are enabled, Soot will not just abort and
//		fail on such an unresolvable reference, but create an empty stub called a
//		phantom class which in turn contains phanom methods to make up for the
//		missing bits and pieces.


		//设置cg pack的选项

		Options.v().setPhaseOption("cg.cha", "on");//不用设置的默认就为true

		Options.v().setPhaseOption("cg.cha", "verbose:true");

		Options.v().setPhaseOption("cg.cha", "apponly:true");

		//Options.v().setPhaseOption("cg.spark", "off");// 默认为fasle 构建cg的选项，spark是一个指向性分析框架 这个打开的会可能会消除一些 节点



		Options.v().set_output_format(Options.output_format_none);

		Options.v().set_process_dir(Collections.singletonList(apkFileLocation));
		
		if (forceAndroidJar)
			Options.v().set_force_android_jar(androidJar);
		else
			Options.v().set_android_jars(androidJar);


		// Set the Soot configuration options. Note that this will needs to be
		// done before we compute the classpath.




		Options.v().set_keep_line_number(true);

		Options.v().set_coffi(true);

		Options.v().set_keep_offset(true);


		//Options.v().set_soot_classpath(apkFileLocation+ File.pathSeparator+"/home/zms/platforms/android-27/android.jar");
		Options.v().set_soot_classpath(getClasspath());//+":/media/softdata/AndroidSDKdirectory/extras/android/support"

		Main.v().autoSetOptions();
		
		// Load whetever we need
		Scene.v().loadNecessaryClasses();

		

	}
}
