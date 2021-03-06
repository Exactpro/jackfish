/*******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.exactprosystems.jf.api.app;

import javax.imageio.ImageIO;
import com.exactprosystems.jf.api.common.Storable;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import java.util.List;

public class ImageWrapper implements Storable
{
	private static final long serialVersionUID = 4378018644528294988L;

	private int					width;
	private int					height;
	private byte[]				bytes;

	public ImageWrapper()
	{
		this.width = 1;
		this.height = 1;
		this.bytes = new byte[0];
	}

	public ImageWrapper(int width, int height, int[] buffer)
	{
		this.width = width;
		this.height = height;
		this.bytes = intToBytes(buffer);
	}

	public ImageWrapper(BufferedImage bi)
	{
		this.width = bi.getWidth();
		this.height = bi.getHeight();
		this.bytes = fileToBytes(bi);
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "{" + this.description + ":" + this.width + "x" + this.height + "}";
	}

	@Override
	public String getName() 
	{
		if (this.fileName == null)
		{
			return "";
		}
		return new File(this.fileName).getName();
	}
	
	@Override
	public List<String> getFileList() 
	{
		String name = getFileName();
		if (name == null)
		{
			name = getDescription();
		}
		if (name == null)
		{
			name = "image";
		}
		
		return Arrays.asList(name);
	}

	@Override
	public byte[] getData(String file) throws IOException
	{
		try (ByteArrayOutputStream temp = new ByteArrayOutputStream())
		{
			ImageIO.write(getImage(), "jpg", temp);
			return temp.toByteArray();
		}
	}

	@Override
	public void addFile(String file, byte[] data) throws IOException 
	{
		setDescription(file);
		this.bytes = data;
		
		try (ByteArrayInputStream bais = new ByteArrayInputStream(data))
		{
			BufferedImage bi = ImageIO.read(bais);
			this.width = bi.getWidth();
			this.height = bi.getHeight();
		}
	}

	public ImageWrapper cutImage(int x1, int y1, int x2, int y2)
	{
		BufferedImage bi = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
		bi.setRGB(0, 0, this.width, this.height, bytesToIntArray(), 0, this.width);
		return new ImageWrapper(bi.getSubimage(x1, y1, x2-x1, y1-y2));
	}

	public BufferedImage getImage()
	{
		BufferedImage bi = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
		bi.setRGB(0, 0, this.width, this.height, bytesToIntArray() , 0, this.width);
		return bi;
	}

	public void saveToFile(String fileName) throws Exception
	{
		this.fileName = fileName;
		try (OutputStream os = new FileOutputStream(new File(fileName)))
		{
			ImageIO.write(this.getImage(), "jpg", os);
		}
	}

	public File saveToDir(String dirName) throws IOException
	{
		File file = new File(dirName);
		File temp = null;
		if (!file.exists())
		{
			file.mkdir();
		}

		if (file.isDirectory())
		{
			temp = File.createTempFile("img", ".jpg", file);
			try (OutputStream os = new FileOutputStream(temp))
			{
				ImageIO.write(this.getImage(), "jpg", os);
			}
			this.fileName = temp.getPath();
		}
		return temp;
	}

    public String getFileName()
	{
		return this.fileName;
	}

    public void clearFile()
    {
        this.fileName = null;
    }

    public String getName(String reportDir) throws IOException
    {
        if (getFileName() == null)
        {
            saveToDir(reportDir);
        }
        return new File(getFileName()).getName();
    }

	public String getDescription()
	{
		return this.description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	private int[] bytesToIntArray() {

		BufferedImage bi = bytesToImage(this.bytes);
		return bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), null, 0, bi.getWidth());
	}

	private byte[] fileToBytes(BufferedImage bi) {

		byte[] res = new byte[0];
		try(ByteArrayOutputStream baos = new ByteArrayOutputStream())
		{
			ImageIO.write(bi, "jpg", baos);
			baos.flush();
			res = baos.toByteArray();
		}
		catch (IOException e)
		{
			//todo logger
		}
		return res;
	}

	private byte[] intToBytes(int[] ints) {
		BufferedImage bi = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
		bi.setRGB(0, 0, this.width, this.height, ints, 0, this.width);

		return fileToBytes(bi);
	}

	private BufferedImage bytesToImage(byte[] bytes) {
		BufferedImage res = null;

		try (InputStream in = new ByteArrayInputStream(bytes)){
			res = ImageIO.read(in);
		}
		catch (IOException e)
		{
			//todo logger
		}

		return res;
	}
	
	private String	fileName	= null;
	private String 	description	= null;
}