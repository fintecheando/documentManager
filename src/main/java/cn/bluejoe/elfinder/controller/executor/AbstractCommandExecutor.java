package cn.bluejoe.elfinder.controller.executor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import cn.bluejoe.elfinder.service.FsService;
import cn.bluejoe.elfinder.util.FsServiceUtils;
import com.github.atave.VaadinCmisBrowser.cmis.api.DocumentView;
import org.apache.chemistry.opencmis.commons.enums.Action;

public abstract class AbstractCommandExecutor implements CommandExecutor
{
	protected void addChildren(Map<String, FsItemEx> map, FsItemEx fsi) throws IOException
	{
		for (FsItemEx f : fsi.listChildren())
		{
			map.put(f.getHash(), f);
		}
	}

	protected void addSubfolders(Map<String, FsItemEx> map, FsItemEx fsi) throws IOException
	{
		for (FsItemEx f : fsi.listChildren())
		{
			if (f.isFolder())
			{
				map.put(f.getHash(), f);
			}
		}
	}

	protected void createAndCopy(FsItemEx src, FsItemEx dst) throws IOException
	{
		if (src.isFolder())
		{
			createAndCopyFolder(src, dst);
		}
		else
		{
			createAndCopyFile(src, dst);
		}
	}

	protected void createAndCopyFile(FsItemEx src, FsItemEx dst) throws IOException
	{
		dst.createFile();
		InputStream is = src.openInputStream();
		OutputStream os = dst.openOutputStream();
		IOUtils.copy(is, os);
		is.close();
		os.close();
	}

	protected void createAndCopyFolder(FsItemEx src, FsItemEx dst) throws IOException
	{
		dst.createFolder();

		for (FsItemEx c : src.listChildren())
		{
			if (c.isFolder())
			{
				createAndCopyFolder(c, new FsItemEx(dst, c.getName()));
			}
			else
			{
				createAndCopyFile(c, new FsItemEx(dst, c.getName()));
			}
		}
	}

	@Override
	public void execute(CommandExecutionContext ctx) throws Exception
	{
		FsService fileService = ctx.getFsServiceFactory().getFileService(ctx.getRequest(), ctx.getServletContext());
		execute(fileService, ctx.getRequest(), ctx.getResponse(), ctx.getServletContext());
	}

	public abstract void execute(FsService fsService, HttpServletRequest request, HttpServletResponse response,
			ServletContext servletContext) throws Exception;

	protected Object[] files2JsonArray(HttpServletRequest request, Collection<FsItemEx> list) throws IOException
	{
		List<Map<String, Object>> los = new ArrayList<Map<String, Object>>();
		for (FsItemEx fi : list)
		{
			los.add(getFsItemInfo(request, fi));
		}

		return los.toArray();
	}
        
        protected Object[] documents2JsonArray(HttpServletRequest request, Collection<DocumentView> list) throws IOException
	{
            System.out.println("Tamaño de la coleccion de documentos "+list.size());
		List<Map<String, Object>> los = new ArrayList<Map<String, Object>>();
		for (DocumentView fi : list)
		{
			los.add(getRepositoryItemInfo(request, fi));
		}

		return los.toArray();
	}

	protected FsItemEx findCwd(FsService fsService, String target) throws IOException
	{
		//current selected directory
		FsItemEx cwd = null;
		if (target != null)
		{
			cwd = findItem(fsService, target);
		}

		if (cwd == null)
			cwd = new FsItemEx(fsService.getVolumes()[0].getRoot(), fsService);

		return cwd;
	}

	protected FsItemEx findItem(FsService fsService, String hash) throws IOException
	{
		return FsServiceUtils.findItem(fsService, hash);
	}

	protected Map<String, Object> getFsItemInfo(HttpServletRequest request, FsItemEx fsi) throws IOException
	{
		Map<String, Object> info = new HashMap<String, Object>();
		info.put("hash", fsi.getHash());
		info.put("mime", fsi.getMimeType());
		info.put("ts", fsi.getLastModified());
		info.put("size", fsi.getSize());
		info.put("read", fsi.isReadable(fsi) ? 1 : 0);
		info.put("write", fsi.isWritable(fsi) ? 1 : 0);
		info.put("locked", fsi.isLocked(fsi) ? 1 : 0);

		if (fsi.getMimeType().startsWith("image"))
		{
			StringBuffer qs = request.getRequestURL();
			info.put("tmb", qs.append(String.format("?cmd=tmb&target=%s", fsi.getHash())));
		}

		if (fsi.isRoot())
		{
			info.put("name", fsi.getVolumnName());
			info.put("volumeid", fsi.getVolumeId());
		}
		else
		{
			info.put("name", fsi.getName());
			info.put("phash", fsi.getParent().getHash());
		}

		if (fsi.isFolder())
		{
			info.put("dirs", fsi.hasChildFolder() ? 1 : 0);
		}

		return info;
	}
        
        protected Map<String, Object> getRepositoryItemInfo(HttpServletRequest request, DocumentView fsi) throws IOException
	{
		Map<String, Object> info = new HashMap<String, Object>();
                System.out.println("fsi.hashCode() "+fsi.hashCode());
		info.put("hash", fsi.hashCode());
                System.out.println("fsi.getMimeType() "+fsi.getMimeType());
		info.put("mime", fsi.getMimeType());
                System.out.println("fsi.getLastModificationDate() "+fsi.getLastModificationDate());
		info.put("ts", fsi.getLastModificationDate());
                System.out.println("fsi.getSize() "+fsi.getSize());
		info.put("size", fsi.getSize());
                System.out.println("fsi.can(Action.CAN_GET_PROPERTIES) "+fsi.can(Action.CAN_GET_PROPERTIES));
		info.put("read", fsi.can(Action.CAN_GET_PROPERTIES) ? 1 : 0);
                System.out.println("fsi.can(Action.CAN_UPDATE_PROPERTIES) "+ fsi.can(Action.CAN_UPDATE_PROPERTIES));
		info.put("write", fsi.can(Action.CAN_UPDATE_PROPERTIES) ? 1 : 0);
                System.out.println("fsi.can(Action.CAN_CHECK_IN) "+fsi.can(Action.CAN_CHECK_IN));
		info.put("locked", fsi.can(Action.CAN_CHECK_IN) ? 1 : 0);

		if (fsi.getMimeType() != null && fsi.getMimeType().startsWith("image"))
		{
                    System.out.println("ES IMAGEN");
			StringBuffer qs = request.getRequestURL();
			info.put("tmb", qs.append(String.format("?cmd=tmb&target=%s", fsi.hashCode())));
		}

		if (fsi.isFolder())
		{
                    System.out.println("ES FOLDER");
			info.put("name", fsi.getName());
			info.put("volumeid", fsi.getId());
		}
		else
		{
                    System.out.println("ES ARCHIVO");
			info.put("name", fsi.getName());
			info.put("phash", fsi.hashCode());
		}

		if (fsi.isFolder())
		{
                    System.out.println("OBTIENE LOS DIRECTORIOS HIJOS");
			info.put("dirs", fsi.asFolder().isRootFolder() ? 1 : 0);
		}
                System.out.println("REGRESA LA INFO");
		return info;
	}

	protected String getMimeDisposition(String mime)
	{
		String[] parts = mime.split("/");
		String disp = ("image".equals(parts[0]) || "text".equals(parts[0]) ? "inline" : "attachments");
		return disp;
	}

	protected Map<String, Object> getOptions(HttpServletRequest request, FsItemEx cwd) throws IOException
	{
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("path", cwd.getPath());
		map.put("disabled", new String[0]);
		map.put("separator", "/");
		map.put("copyOverwrite", 1);
		map.put("archivers", new Object[0]);

		return map;
	}
}