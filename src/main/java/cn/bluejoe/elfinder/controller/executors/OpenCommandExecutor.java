package cn.bluejoe.elfinder.controller.executors;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.CommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import cn.bluejoe.elfinder.service.FsVolume;
import com.github.atave.VaadinCmisBrowser.cmis.api.CmisClient;
import com.github.atave.VaadinCmisBrowser.cmis.api.DocumentView;
import com.github.atave.VaadinCmisBrowser.cmis.api.FolderView;
import com.github.atave.VaadinCmisBrowser.cmis.impl.OpenCmisInMemoryClient;
import java.util.Collection;
import java.util.HashSet;
import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;

public class OpenCommandExecutor extends AbstractJsonCommandExecutor implements CommandExecutor
{
	@Override
	public void execute(FsService fsService, HttpServletRequest request, ServletContext servletContext, JSONObject json)
			throws Exception
	{
		boolean init = request.getParameter("init") != null;
		boolean tree = request.getParameter("tree") != null;
		String target = request.getParameter("target");
                
                CmisClient client;
                Collection<DocumentView> results = new HashSet<>();
                try {
                    client = new OpenCmisInMemoryClient("test", "test");
                    // Get the current folder
                    FolderView currentFolder = client.getCurrentFolder();
                    //Get the Documents in the Folder
                    results = currentFolder.getDocuments();
                    //Just to check the File Names
                    for(DocumentView document : results) {
                        // do something with the document
                        System.out.println("NOMBRE ARCHIVO "+document.getName());
                    }
                } catch(CmisBaseException e) {
                    e.printStackTrace();// Wrong username and password
                }
                                
		Map<String, FsItemEx> files = new LinkedHashMap<String, FsItemEx>();
		if (init)
		{
			json.put("api", 2.1);
			json.put("netDrivers", new Object[0]);
		}

		if (tree)
		{
			for (FsVolume v : fsService.getVolumes())
			{
				FsItemEx root = new FsItemEx(v.getRoot(), fsService);
                                System.out.println("ROOT "+root +" hash "+ root.getHash());
				files.put(root.getHash(), root);
				addSubfolders(files, root);
			}
		}

		FsItemEx cwd = findCwd(fsService, target);
                System.out.println("CWD "+cwd +" hash "+ cwd.getHash());
		files.put(cwd.getHash(), cwd);
		addChildren(files, cwd);
                System.out.println(files.values());
		json.put("files", files2JsonArray(request, files.values()));
                //json.put("files", documents2JsonArray(request, results));
                System.out.println("LLENA EL CWD");
                //json.put("cwd", getRepositoryItemInfo(request, cwd));
		json.put("cwd", getFsItemInfo(request, cwd));
                System.out.println("OPCIONES");
		json.put("options", getOptions(request, cwd));
	}
}
