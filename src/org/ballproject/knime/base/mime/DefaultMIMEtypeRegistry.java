/*
 * Copyright (c) 2011, Marc Röttig.
 *
 * This file is part of GenericKnimeNodes.
 * 
 * GenericKnimeNodes is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.ballproject.knime.base.mime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ballproject.knime.base.mime.demangler.Demangler;
import org.knime.core.data.DataType;


public class DefaultMIMEtypeRegistry implements MIMEtypeRegistry
{
	protected List<MIMEtypeRegistry>   resolvers  = new ArrayList<MIMEtypeRegistry>();
	protected Map<DataType,Demangler>  demanglers = new HashMap<DataType,Demangler>(); 
	protected Map<DataType,Demangler>  manglers   = new HashMap<DataType,Demangler>();
	
	@Override
	public MIMEFileCell getCell(String name)
	{
		MIMEFileCell cell = null;
		for(MIMEtypeRegistry resolver: resolvers)
		{
			MIMEFileCell rc = resolver.getCell(name); 
			if(rc!=null)
				cell = rc; 
		}
		return cell;
	}

	@Override
	public void addResolver(MIMEtypeRegistry resolver)
	{
		resolvers.add(resolver);
	}

	@Override
	public MIMEtype getMIMEtype(String name)
	{
		MIMEtype mt = null;
		for(MIMEtypeRegistry resolver: resolvers)
		{
			MIMEtype rmt = resolver.getMIMEtype(name); 
			if(rmt!=null)
				mt = rmt; 
		}
		return mt;
	}

	@Override
	public Demangler getDemangler(DataType type)
	{
		return demanglers.get(type);
	}

	@Override
	public void addDemangler(Demangler demangler)
	{
		demanglers.put(demangler.getSourceType(), demangler);
		manglers.put(demangler.getTargetType(), demangler);
	}

	@Override
	public Demangler getMangler(DataType type)
	{
		return manglers.get(type);
	}

}
