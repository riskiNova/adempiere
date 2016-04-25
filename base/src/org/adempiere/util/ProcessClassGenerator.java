/**
 * 
 */
package org.adempiere.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.Adempiere;
import org.compiere.model.I_AD_Process_Para;
import org.compiere.model.MProcess;
import org.compiere.model.MProcessPara;
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;

/**
 * 	Generate Process Classes extending SvrProcess.
 *	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<li> FR [ 326 ] Process source code generated automatically
 *		@see https://github.com/adempiere/adempiere/issues/326
 *	@author Victor Perez, victor.perez@e-evolution.com, http://e-evolution.com
 */
public class ProcessClassGenerator {

	/**
	 * Standard constructor
	 * @param process
	 * @param directory
	 */
	public ProcessClassGenerator(MProcess process, String directory) {
		//	Get Values
		processId = process.getAD_Process_ID();
		className = process.getClassname();
		processName = process.getName();
		directoryName = directory;
	}
	
	/**	Process ID		*/
	private int processId = 0;
	/**	Process Name	*/
	private String processName = null;
	/**	Class Name		*/
	private String className = null;
	/**	Directory Name	*/
	private String directoryName = null;
	
	/**
	 * Create file
	 * @return
	 */
	public String createFile() {
		int index = className.lastIndexOf(".");
		if(index == -1)
			throw new AdempiereException("@Classname@ @NotFound@");
		//	
		String packageName = className.substring(0, index);
		String fileName = className.substring(index + 1) + "Abstract";
		
		StringBuffer header = createHeader(processId, packageName, fileName);
		// Save
		if (!directoryName.endsWith(File.separator) )
			directoryName += File.separator;
		//	Write to file
		writeToFile(header, directoryName + fileName + ".java");
		//	Return
		return directoryName + fileName + ".java";
	}
	
	/** Import classes 		*/
	private Collection<String> importClasses = new TreeSet<String>();
	/**	Logger			*/
	private static CLogger	log	= CLogger.getCLogger (ProcessClassGenerator.class);
	/** Parameters Name	*/
	private StringBuffer parametersName = new StringBuffer();
	/** Parameters Value	*/
	private StringBuffer parametersValue = new StringBuffer();
	/** Parameters Fill	*/
	private StringBuffer parametersFill = new StringBuffer();
	/** Parameters Getters */
	private StringBuffer parametersGetter = new StringBuffer();
	/** Parameters **/
	private MProcessPara[] parameters;
	
	/**
	 * Create Parameters for header
	 * @return
	 */
	private void createParameters() {

		//	Create Name and Values
		for(MProcessPara parameter : getParameters()) {
			createParameterName(parameter);
			createParameterValue(parameter, false);
			createParameterFill(parameter, false);

			//	For Range
			if(parameter.isRange()) {
				createParameterValue(parameter, true);
				createParameterFill(parameter, true);
			}
		}
	}

	/**
	 * Create Parameters for header
	 * @return
	 */
	private void createParameterGetter() {

		//	Create Name and Values
		for(MProcessPara parameter : getParameters()) {
			createGetterParameter(parameter , false);
			//	For Range
			if(parameter.isRange()) {
				createGetterParameter(parameter, true);
			}
		}
	}

	private MProcessPara[] getParameters()
	{
		if (parameters != null && parameters.length > 0)
			return parameters;

		MProcess process = new MProcess(Env.getCtx() , processId, null);
		parameters = process.getParameters();
		return  parameters;
	}
	/**
	 * Create Header class
	 * @param processId
	 * @param packageName
	 * @return
	 */
	private StringBuffer createHeader(int processId, String packageName, String className) {
		StringBuffer header = new StringBuffer();
		createParameters();
		//	Add SvrProcess
		if(!packageName.equals("org.compiere.process"))
			addImportClass(SvrProcess.class);
		//	Add license
		header.append(ModelInterfaceGenerator.COPY);
		//	New line
		header.append(ModelInterfaceGenerator.NL);
		//	Package
		header.append("package ").append(packageName).append(";\n");
		//	New line
		header.append(ModelInterfaceGenerator.NL);
		//	Import Class
		header.append(getImportClass());
		//	New line
		header.append(ModelInterfaceGenerator.NL);
		//	New line
		header.append(ModelInterfaceGenerator.NL);
		//	Add comments
		header.append("\n/** Generated Process for (").append(processName).append(")\n")
		 	.append(" *  @author ADempiere (generated) \n")
		 	.append(" *  @version ").append(Adempiere.MAIN_VERSION).append("\n")
		 	.append(" */\n");
		//	Add Class Name
		header
			.append("public abstract class ").append(className).append(" extends ").append("SvrProcess")
			.append("\n{");

		header.append("\n\t/** Process Name \t*/");
		header.append("\n\tpublic static final String NAME = ").append("\"").append(processName.trim()).append("\";");
		header.append("\n\t/** Process Id \t*/");
		header.append("\n\tpublic static final int ID = ").append(processId).append(";");
		header.append("\n ");

		//	Add Parameters Name
		header.append(parametersName);
		//	New line
		header.append(ModelInterfaceGenerator.NL);
		//	Add Parameters Value
		header.append(parametersValue);

		//	Add Prepare method
		header.append("\n ")
			.append("\n\n\t@Override")
			.append("\n\tprotected void prepare()")
			.append("\n\t{");
		//	Add fill
		header.append(parametersFill);
		//	End Prepare
		header.append("\n\t}");
		//	Add do it
		//header
		//	.append("\n\n\t@Override")
		//	.append("\n\tprotected abstract String doIt() throws Exception;");
			//.append("\n\t{")
			//.append("\n\t\treturn \"\";")
			//.append("\n\t}");
		createParameterGetter();
		header.append(parametersGetter);

		//	End class
		header.append("\n}");
		//	Return
		return header;
	}

	/**
	 * create Parameter Name
	 * @param parameter
     */
	private void createParameterName(MProcessPara parameter) {
		//	Add new Line
		parametersName.append(ModelInterfaceGenerator.NL);
		//	Add Comment
		parametersName
			.append("\t/**\tParameter Name for ").append(parameter.getColumnName()).append("\t*/")
			.append(ModelInterfaceGenerator.NL)
			.append("\tpublic static final String ").append(parameter.getColumnName())
			.append(" = ").append("\"").append(parameter.getColumnName())
			.append("\";");
	}
	
	/**
	 * Create Comment and parameter Value
	 * @param parameter
	 * @param isTo
	 * @param isTo
	 */
	private void createParameterValue(MProcessPara parameter, boolean isTo) {
		//	Add new Line
		parametersValue.append(ModelInterfaceGenerator.NL);
		String variableName = getVariableName(parameter);
		//	Add Comment
		parametersValue
			.append("\t/**\tParameter Value for ").append(variableName).append(isTo ? "To": "").append("\t*/")
			.append(ModelInterfaceGenerator.NL)
			.append("\tprivate ").append(getType(parameter)).append(" ")
			.append(variableName.trim())
			.append(isTo ? "To": "")
			.append(";");
	}

	/**
	 * Create Comment and parameter Value
	 * @param parameter
	 * @param isTo
	 * @param isTo
	 */
	private void createGetterParameter(MProcessPara parameter, boolean isTo) {
		//	Add new Line
		parametersGetter.append(ModelInterfaceGenerator.NL);
		String variableName = getVariableName(parameter);
		//	Add Comment
		parametersGetter
				.append("\t/**\t Getter Parameter Value for ").append(variableName).append(isTo ? "To": "").append("\t*/")
				.append(ModelInterfaceGenerator.NL)
				.append("\tprotected ").append(getType(parameter)).append(" ").append(getMethodName(parameter))
				.append(isTo ? "To": "")
				.append("() {")
				.append("\n\t\treturn ").append(variableName.trim())
				.append(isTo ? "To": "")
				.append(";\n")
				.append("\t}");
	}
	/**
	 * Create Fill Source
	 * @param parameter
	 * @param isTo
	 */
	private void createParameterFill(MProcessPara parameter, Boolean isTo) {
		//	Add new Line
		parametersFill.append(ModelInterfaceGenerator.NL);
		String variableName = getVariableName(parameter);
		//	Add Comment
		parametersFill
			.append("\t\t").append(variableName)
			.append(isTo ? "To": "")
			.append(" = ").append(getProcessMethod(parameter, isTo))
			.append("(").append(parameter.getColumnName()).append(")")
			.append(";");
	}

	private String getVariableName(MProcessPara parameter)
	{
		String parameterName = getParameterName(parameter);
		StringBuilder variableName = new StringBuilder();
		if ((DisplayType.List == parameter.getAD_Reference_ID() && 319 == parameter.getAD_Reference_Value_ID()) || DisplayType.YesNo == parameter.getAD_Reference_ID())
			variableName.append("is").append(parameterName);
		else
			variableName.append(parameterName.substring(0 ,1).toLowerCase()).append(parameterName.substring(1,getParameterName(parameter).length()));
		if (DisplayType.isLookup(parameter.getAD_Reference_ID()) && DisplayType.List != parameter.getAD_Reference_ID())
			variableName.append("Id");

		return variableName.toString();
	}

	private String getMethodName(MProcessPara parameter)
	{
		String parameterName = getParameterName(parameter);
		StringBuilder variableName = new StringBuilder();
		if ((DisplayType.List == parameter.getAD_Reference_ID() && 319 == parameter.getAD_Reference_Value_ID()) || DisplayType.YesNo == parameter.getAD_Reference_ID())
			variableName.append("is").append(parameterName);
		else
			variableName.append("get").append(parameterName);
		if (DisplayType.isLookup(parameter.getAD_Reference_ID()) && DisplayType.List != parameter.getAD_Reference_ID())
			variableName.append("Id");

		return variableName.toString();
	}
	
	/**
	 * Get Type for declaration
	 * @param parameter
	 * @return
	 */
	private String getType(MProcessPara parameter) {
		Class clazz = DisplayType.getClass(parameter.getAD_Reference_ID(), true);
		//	Verify Type
		if (clazz == String.class && DisplayType.isText(parameter.getAD_Reference_ID())) {
			return "String";
		} else if (clazz == Integer.class) {
			return "int";
		} else if (clazz == BigDecimal.class) {
			addImportClass(BigDecimal.class);
			return "BigDecimal";
		} else if (clazz == Timestamp.class) {
			addImportClass(Timestamp.class);
			return "Timestamp";
		} else if (clazz == Boolean.class || parameter.getAD_Reference_Value_ID() == 319 || DisplayType.YesNo == parameter.getAD_Reference_ID()) {
			return "boolean";
		} else if (DisplayType.List == parameter.getAD_Reference_ID())
			return "String";
		//
		return "Object";
	}
	
	/**
	 * Get Type for declaration
	 * @param parameter
	 * @return
	 */
	private String getProcessMethod(MProcessPara parameter, boolean isTo) {
		String type = getType(parameter);
		//	
		String typeForMethod = type.substring(0, 1);
		//	Change first
		typeForMethod = typeForMethod.toUpperCase();
		//	
		typeForMethod = typeForMethod + type.substring(1);
		//	Return
		if(typeForMethod.equals("Object"))
			return "getParameter";
		//	Default return
		return "getParameter" + (isTo? "To": "") + "As" + typeForMethod;
	}
	
	
	/**
	 * Add class name to class import list
	 * @param className
	 */
	private void addImportClass(String className) {
		if (className == null
				|| (className.startsWith("java.lang.") && !className.startsWith("java.lang.reflect.")))
			return;
		for(String name : importClasses) {
			if (className.equals(name))
				return;
		}
		importClasses.add(className);
	}
	
	/**
	 * Add class to class import list
	 * @param cl
	 */
	private void addImportClass(Class<?> cl) {
		if (cl.isArray()) {
			cl = cl.getComponentType();
		}
		if (cl.isPrimitive())
			return;
		addImportClass(cl.getCanonicalName());
	}
	
	/**
	 * Get import class for header
	 * @return
	 */
	private StringBuffer getImportClass() {
		StringBuffer importClass = new StringBuffer();
		for(String imp : importClasses) {
			//	For new line
			if(importClass.length() > 0)
				importClass.append(ModelInterfaceGenerator.NL);
			//	
			importClass.append("import ").append(imp).append(";");
		}
		//	Default return
		return importClass;
	}
	
	/**************************************************************************
	 * 	Write to file
	 * 	@param sb string buffer
	 * 	@param fileName file name
	 */
	private void writeToFile (StringBuffer sb, String fileName)
	{
		try
		{
			File out = new File (fileName);
			Writer fw = new OutputStreamWriter(new FileOutputStream(out, false), "UTF-8");
			for (int i = 0; i < sb.length(); i++)
			{
				char c = sb.charAt(i);
				//	after
				if (c == ';' || c == '}')
				{
					fw.write (c);
				}
				//	before & after
				else if (c == '{')
				{
					fw.write (c);
				}
				else
					fw.write (c);
			}
			fw.flush ();
			fw.close ();
			float size = out.length();
			size /= 1024;
			log.info(out.getAbsolutePath() + " - " + size + " kB");
		}
		catch (Exception ex)
		{
			log.log(Level.SEVERE, fileName, ex);
			throw new RuntimeException(ex);
		}
	}

	private String  getParameterName(MProcessPara processParameter)
	{
		String parameterName = processParameter.getName().replaceAll("\\s", "").replaceAll("_", "").replaceAll("/","");
		return parameterName;
	}
}
