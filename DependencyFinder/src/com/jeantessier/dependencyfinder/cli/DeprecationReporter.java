/*
 *  Copyright (c) 2001-2003, Jean Tessier
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

package com.jeantessier.dependencyfinder.cli;

import java.io.*;
import java.util.*;

import org.apache.log4j.*;

import com.jeantessier.commandline.*;
import com.jeantessier.dependency.*;
import com.jeantessier.dependencyfinder.*;

public class DeprecationReporter {
	public static final String DEFAULT_LOGFILE         = "System.out";

	public static void Error(CommandLineUsage clu, String msg) {
		System.err.println(msg);
		Error(clu);
	}

	public static void Error(CommandLineUsage clu) {
		System.err.println(clu);
		System.err.println();
		System.err.println("Defaults is text output to the console.");
		System.err.println();
	}

	public static void Version() throws IOException {
		Version version = new Version();
		
		System.err.print(version.ImplementationTitle());
		System.err.print(" ");
		System.err.print(version.ImplementationVersion());
		System.err.print(" (c) ");
		System.err.print(version.CopyrightDate());
		System.err.print(" ");
		System.err.print(version.CopyrightHolder());
		System.err.println();
		
		System.err.print(version.ImplementationURL());
		System.err.println();
		
		System.err.print("Compiled on ");
		System.err.print(version.ImplementationDate());
		System.err.println();
	}

	public static void main(String[] args) throws Exception {
		// Parsing the command line
		CommandLine command_line = new CommandLine(new AtLeastParameterStrategy(1));
		command_line.AddMultipleValuesSwitch("includes", true);
		command_line.AddMultipleValuesSwitch("excludes");
		command_line.AddToggleSwitch("xml");
		command_line.AddToggleSwitch("validate");
		command_line.AddSingleValueSwitch("encoding",   XMLPrinter.DEFAULT_ENCODING);
		command_line.AddSingleValueSwitch("dtd-prefix", XMLPrinter.DEFAULT_DTD_PREFIX);
		command_line.AddSingleValueSwitch("indent-text");
		command_line.AddToggleSwitch("time");
		command_line.AddSingleValueSwitch("out");
		command_line.AddToggleSwitch("help");
		command_line.AddOptionalValueSwitch("verbose",  DEFAULT_LOGFILE);
		command_line.AddToggleSwitch("version");

		CommandLineUsage usage = new CommandLineUsage("DeprecationReporter");
		command_line.Accept(usage);

		try {
			command_line.Parse(args);
		} catch (IllegalArgumentException ex) {
			Error(usage, ex.toString());
			System.exit(1);
		} catch (CommandLineException ex) {
			Error(usage, ex.toString());
			System.exit(1);
		}

		if (command_line.ToggleSwitch("help")) {
			Error(usage);
		}
		
		if (command_line.ToggleSwitch("version")) {
			Version();
		}

		if (command_line.ToggleSwitch("help") || command_line.ToggleSwitch("version")) {
			System.exit(1);
		}

		VerboseListener verbose_listener = new VerboseListener();
		if (command_line.IsPresent("verbose")) {
			if ("System.out".equals(command_line.OptionalSwitch("verbose"))) {
				verbose_listener.Writer(System.out);
			} else {
				verbose_listener.Writer(new FileWriter(command_line.OptionalSwitch("verbose")));
			}
		}

		/*
		 *  Beginning of main processing
		 */

		Date start = new Date();

		Collection collection = new HashSet();
		Iterator   i;

		i = command_line.MultipleSwitch("includes").iterator();
		while (i.hasNext()) {
			BufferedReader reader = new BufferedReader(new FileReader(i.next().toString()));
			String line;
			while ((line = reader.readLine()) != null) {
				collection.add(line);
			}
			reader.close();
		}
		
		i = command_line.MultipleSwitch("excludes").iterator();
		while (i.hasNext()) {
			BufferedReader reader = new BufferedReader(new FileReader(i.next().toString()));
			String line;
			while ((line = reader.readLine()) != null) {
				collection.remove(line);
			}
			reader.close();
		}

		SelectionCriteria scope_criteria = new CollectionSelectionCriteria(collection);
		SelectionCriteria filter_criteria = new ComprehensiveSelectionCriteria();

		GraphCopier copier = new GraphCopier(new SelectiveTraversalStrategy(scope_criteria, filter_criteria));
		
		i = command_line.Parameters().iterator();
		while (i.hasNext()) {
			String filename = (String) i.next();
			Logger.getLogger(DeprecationReporter.class).info("Reading " + filename);
			verbose_listener.Print("Reading " + filename);

			Collection packages = Collections.EMPTY_LIST;

			if (filename.endsWith(".xml")) {
				NodeLoader loader = new NodeLoader(command_line.ToggleSwitch("validate"));
				loader.addDependencyListener(verbose_listener);
				packages = loader.Load(filename).Packages().values();
			}

			Logger.getLogger(DeprecationReporter.class).info("Read in " + packages.size() + " package(s) from \"" + filename + "\".");

			copier.TraverseNodes(packages);
		}

		Logger.getLogger(DeprecationReporter.class).info("Reporting " + copier.ScopeFactory().Packages().values().size() + " package(s) ...");
	
		verbose_listener.Print("Printing the graph ...");

		PrintWriter out;
		if (command_line.IsPresent("out")) {
			out = new PrintWriter(new FileWriter(command_line.SingleSwitch("out")));
		} else {
			out = new PrintWriter(System.out);
		}

		Printer printer;
		if (command_line.IsPresent("xml")) {
			printer = new XMLPrinter(out, command_line.SingleSwitch("encoding"), command_line.SingleSwitch("dtd-prefix"));
		} else {
			TextPrinter text_printer = new TextPrinter(out);
			text_printer.ShowInbounds(true);
			text_printer.ShowOutbounds(false);
			text_printer.ShowEmptyNodes(false);
			
			printer = text_printer;
		}
			
		if (command_line.IsPresent("indent-text")) {
			printer.IndentText(command_line.SingleSwitch("indent-text"));
		}
	    
		printer.TraverseNodes(copier.ScopeFactory().Packages().values());

		out.close();

		Date end = new Date();

		if (command_line.ToggleSwitch("time")) {
			System.err.println(DeprecationReporter.class.getName() + ": " + ((end.getTime() - (double) start.getTime()) / 1000) + " secs.");
		}

		verbose_listener.Close();
	}
}