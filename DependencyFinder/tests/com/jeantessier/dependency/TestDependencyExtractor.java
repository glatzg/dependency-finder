/*
 *  Copyright (c) 2001-2004, Jean Tessier
 *  All rights reserved.
 *  
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *  
 *  	* Redistributions of source code must retain the above copyright
 *  	  notice, this list of conditions and the following disclaimer.
 *  
 *  	* Redistributions in binary form must reproduce the above copyright
 *  	  notice, this list of conditions and the following disclaimer in the
 *  	  documentation and/or other materials provided with the distribution.
 *  
 *  	* Neither the name of Jean Tessier nor the names of his contributors
 *  	  may be used to endorse or promote products derived from this software
 *  	  without specific prior written permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.jeantessier.dependency;

import java.io.*;
import java.util.*;

import junit.framework.*;

import org.apache.log4j.*;
import org.apache.oro.text.perl.*;

import com.jeantessier.classreader.*;

public class TestDependencyExtractor extends TestCase {
	public static final String TEST_CLASS    = "test";
	public static final String TEST_FILENAME = "classes" + File.separator + "test.class";
	
	NodeFactory factory;
	
	Node _package;
	Node test_class;
	Node test_main_feature;
	Node test_test_feature;
		
	Node java_io_package;
	Node java_io_PrintStream_class;
	Node java_io_PrintStream_println_feature;
	
	Node java_lang_package;
	Node java_lang_NullPointerException_class;
	Node java_lang_Object_class;
	Node java_lang_Object_Object_feature;
	Node java_lang_String_class;
	Node java_lang_System_class;
	Node java_lang_System_out_feature;
		
	Node java_util_package;
	Node java_util_Collections_class;
	Node java_util_Collections_singleton_feature;
	Node java_util_Set_class;

	ClassfileLoader loader;
	NodeFactory     test_factory;

	protected void setUp() throws Exception {
		Logger.getLogger(getClass()).info("Starting test: " + getName());

		factory = new NodeFactory();

		_package = factory.createPackage("");
		test_class = factory.createClass("test");
		test_main_feature = factory.createFeature("test.main(java.lang.String[])");
		test_test_feature = factory.createFeature("test.test()");
		
		java_io_package = factory.createPackage("java.io");
		java_io_PrintStream_class = factory.createClass("java.io.PrintStream");
		java_io_PrintStream_println_feature = factory.createFeature("java.io.PrintStream.println(java.lang.Object)");

		java_lang_package = factory.createPackage("java.lang");
		java_lang_NullPointerException_class = factory.createClass("java.lang.NullPointerException");
		java_lang_Object_class = factory.createClass("java.lang.Object");
		java_lang_Object_Object_feature = factory.createFeature("java.lang.Object.Object()");
		java_lang_String_class = factory.createClass("java.lang.String");
		java_lang_System_class = factory.createClass("java.lang.System");
		java_lang_System_out_feature = factory.createFeature("java.lang.System.out");
		
		java_util_package = factory.createPackage("java.util");
		java_util_Collections_class = factory.createClass("java.util.Collections");
		java_util_Collections_singleton_feature = factory.createFeature("java.util.Collections.singleton(java.lang.Object)");
		java_util_Set_class = factory.createClass("java.util.Set");
		
		test_class.addDependency(java_lang_Object_class);
		test_main_feature.addDependency(java_io_PrintStream_class);
		test_main_feature.addDependency(java_io_PrintStream_println_feature);
		test_main_feature.addDependency(java_lang_NullPointerException_class);
		test_main_feature.addDependency(java_lang_Object_class);
		test_main_feature.addDependency(java_lang_Object_Object_feature);
		test_main_feature.addDependency(java_lang_String_class);
		test_main_feature.addDependency(java_lang_System_out_feature);
		test_main_feature.addDependency(java_util_Collections_singleton_feature);
		test_main_feature.addDependency(java_util_Set_class);
		test_test_feature.addDependency(java_lang_Object_Object_feature);

		loader = new AggregatingClassfileLoader();
		loader.load(Collections.singleton(TEST_FILENAME));

		test_factory = new NodeFactory();
		loader.getClassfile(TEST_CLASS).accept(new CodeDependencyCollector(test_factory));
	}

	protected void tearDown() throws Exception {
		Logger.getLogger(getClass()).info("End of " + getName());
	}
	
	public void testPackageList() {
		assertEquals("Different list of packages",
					 factory.getPackages().keySet(),
					 test_factory.getPackages().keySet());
	}
	
	public void testClassList() {
		assertEquals("Different list of classes",
					 factory.getClasses().keySet(),
					 test_factory.getClasses().keySet());
	}
	
	public void testFeatureList() {
		assertEquals("Different list of features",
					 factory.getFeatures().keySet(),
					 test_factory.getFeatures().keySet());
	}
	
	public void testPackages() {
		Iterator i = factory.getPackages().keySet().iterator();
		while (i.hasNext()) {
			Object key = i.next();
			assertEquals(factory.getPackages().get(key), test_factory.getPackages().get(key));
			assertTrue(key + " is same", factory.getPackages().get(key) != test_factory.getPackages().get(key));
			assertEquals(key + " inbounds",
						 ((Node) factory.getPackages().get(key)).getInboundDependencies().size(),
						 ((Node) test_factory.getPackages().get(key)).getInboundDependencies().size());
			assertEquals(key + " outbounds",
						 ((Node) factory.getPackages().get(key)).getOutboundDependencies().size(),
						 ((Node) test_factory.getPackages().get(key)).getOutboundDependencies().size());
		}
	}
	
	public void testClasses() {
		Iterator i = factory.getClasses().keySet().iterator();
		while (i.hasNext()) {
			Object key = i.next();
			assertEquals(factory.getClasses().get(key), test_factory.getClasses().get(key));
			assertTrue(key + " is same", factory.getClasses().get(key) != test_factory.getClasses().get(key));
			assertEquals(key + " inbounds",
						 ((Node) factory.getClasses().get(key)).getInboundDependencies().size(),
						 ((Node) test_factory.getClasses().get(key)).getInboundDependencies().size());
			assertEquals(key + " outbounds",
						 ((Node) factory.getClasses().get(key)).getOutboundDependencies().size(),
						 ((Node) test_factory.getClasses().get(key)).getOutboundDependencies().size());
		}
	}
	
	public void testFeatures() {
		Iterator i = factory.getFeatures().keySet().iterator();
		while (i.hasNext()) {
			Object key = i.next();
			assertEquals(factory.getFeatures().get(key), test_factory.getFeatures().get(key));
			assertTrue(key + " is same", factory.getFeatures().get(key) != test_factory.getFeatures().get(key));
			assertEquals(key + " inbounds",
						 ((Node) factory.getFeatures().get(key)).getInboundDependencies().size(),
						 ((Node) test_factory.getFeatures().get(key)).getInboundDependencies().size());
			assertEquals(key + " outbounds",
						 ((Node) factory.getFeatures().get(key)).getOutboundDependencies().size(),
						 ((Node) test_factory.getFeatures().get(key)).getOutboundDependencies().size());
		}
	}

	public void testStaticInitializer() throws IOException {
		ClassfileLoader loader  = new AggregatingClassfileLoader();
		NodeFactory     factory = new NodeFactory();
		
		loader.load(Collections.singleton("classes" + File.separator + "StaticInitializerTest.class"));

		Classfile classfile = loader.getClassfile("StaticInitializerTest");
		classfile.accept(new CodeDependencyCollector(factory));

		Collection feature_names = factory.getFeatures().keySet();
		
		Iterator i = classfile.getAllMethods().iterator();
		while (i.hasNext()) {
			Method_info method = (Method_info) i.next();
			assertTrue("Missing method " + method.getFullSignature(), feature_names.contains(method.getFullSignature()));
		}
	}
}
