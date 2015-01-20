import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.apache.pdfbox.PDFToImage;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfNumber;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfRectangle;

// Works around BufferedImages but with some convenient tools
import edu.virginia.cs.cs1110.multimedia.Picture;
import edu.virginia.cs.cs1110.multimedia.Pixel;

/*
 	This program used the Apache PDFBox 1.8.2 API under the Apache License version 2.0:
 	
 	Copyright 2013 Justin Smalkowski

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
 	
 	///////////////////////////////////////////////////////////////////////////////////
 	
	This program also used the iText PDF 5.4.2 API under the AGPL license:
	
	Copyright (C) 2013  Justin Smalkowski
	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU Affero General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	
	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Affero General Public License for more details.
	
	You should have received a copy of the GNU Affero General Public License
	along with this program.  If not, see http://www.gnu.org/licenses/.
*/

public class Separator {

	static PdfReader pReader;
	static String[] PDFtoImageArgs;
	static File picsFolder;
	static File tempFolder;
	static boolean verFlag;
	static int split = 0;
	static boolean doubFlag;
	static boolean doubSided;
	static int runCount = 0;
	static final String scanFolder = System.getProperty("user.home") + "\\Desktop\\Scan Folder\\";
	static ArrayList<File> finalScans;

	// Create a png or gif from each page to use the Picture and Pixel Classes
	public static void main(String[] args) throws Exception {
		File scanFolderList = new File(scanFolder);
		File[] scans1st = scanFolderList.listFiles();
		finalScans = new ArrayList<File>();
		for (File f : scans1st) {
			if (!f.isDirectory() && !f.getName().equalsIgnoreCase("PDFSeparator1.7.jar")) {
				run(f);
			}
		}
	}

	public static void run(File file){
		String name = file.getName();
		Object[] options = { "Vertical", "Horizontal", "Cancel" };
		String fileName = "";
		int answer = JOptionPane
				.showOptionDialog(
						null,
						"Before you begin separating a PDF file, place it in a folder called \"Scan Folder\" \non the desktop. Select the orientation of the "
								+ "first page of " + name + " below.\n\n"
								+ "(Clicking Cancel will stop the process.)",
						"PDFSeparator", JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE, null, options, options[2]);
		if (answer == JOptionPane.CANCEL_OPTION
				|| answer == JOptionPane.CLOSED_OPTION) {
			System.exit(0);
		}
		if (answer == JOptionPane.YES_OPTION) {
			verFlag = true;
		}
		if (answer == JOptionPane.NO_OPTION) {
			verFlag = false;
		}
		
		try {
			Object[] options2 = { "Single-sided", "Double-sided", "Cancel" };
			int answer2 = JOptionPane.showOptionDialog(null,
					"Is " + name + " single-sided or double-sided?\n"
							+ "(Clicking Cancel will stop the process.)",
					"PDFSeparator", JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, options2, options2[2]);
			if (answer2 == JOptionPane.CANCEL_OPTION
					|| answer == JOptionPane.CLOSED_OPTION) {
				System.exit(0);
			}
			if (answer2 == JOptionPane.YES_OPTION) {
				doubSided = false;
			}
			if (answer2 == JOptionPane.NO_OPTION) {
				doubSided = true;
			}
			fileName = file.getPath();
			// Creating main scan File
			
			pReader = new PdfReader(fileName);

			// Creating temp pictures folder
			File scanPics = new File(scanFolder + "Temp Pictures\\temp.txt");
			picsFolder = new File(scanFolder + "Temp Pictures");
			File tempFolderFile = new File(scanFolder + "Temp PDFs\\temp2.txt");
			tempFolder = new File(scanFolder + "Temp PDFs");
			scanPics.getParentFile().mkdirs();
			tempFolderFile.getParentFile().mkdirs();
			FileWriter writer = new FileWriter(scanPics);
			FileWriter writer2 = new FileWriter(tempFolderFile);
			writer.close();
			writer2.close();
			scanPics.delete();
			tempFolderFile.delete();

			// config option 1:convert all document to image
			PDFtoImageArgs = new String[3];
			PDFtoImageArgs[0] = "-outputPrefix";
			PDFtoImageArgs[1] = System.getProperty("user.home")
					+ "\\Desktop\\Scan Folder\\Temp Pictures\\my_image_";
			PDFtoImageArgs[2] = fileName;
		} catch (Exception e) {
			JOptionPane
					.showMessageDialog(
							null,
							"Make sure the PDF  is in a desktop folder \nnamed"
									+ "\"Scan Folder\", then try again.",
							"Error!", JOptionPane.OK_OPTION);
			System.exit(0);
		}

		try {
			PDFToImage.main(PDFtoImageArgs);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null,
					"There was an error while trying to read\n"
							+ "the PDF pages as images.", "Error!",
					JOptionPane.OK_OPTION);
			System.exit(0);
		}

		splitPDFFile(fileName);

		// Clearing Temp Pictures & deleting the folder
		File[] files = picsFolder.listFiles();
		for (File f : files) {
			f.delete();
		}
		picsFolder.delete();
		pReader.close();
	}
	
	public static int getOrientation(Picture pic) throws IOException {
		boolean vertical = false;
		boolean horizontal = false;
		int horizontalSum = 0;
		int verticalSum = 0;
		Desktop dt = Desktop.getDesktop();
		File picture = new File(pic.getFileName());
		int orientation = -1;
		
		// Calculating # of non-white pixels to determine orientation
		for (int x = 0; x <= 900; x++) {
			for (int y = 855; y <= 1035; y++) {
				Pixel p = pic.getPixel(x, y);
				if (p.getRed() + p.getBlue() + p.getGreen() <= 740) {
					verticalSum++;
				}
			}
		}
		for (int x = 890; x <= 1100; x++) {
			for (int y = 100; y <= 850; y++) {
				Pixel p = pic.getPixel(x, y);
				if (p.getRed() + p.getBlue() + p.getGreen() <= 740) {
					horizontalSum++;
				}
			}
		}
		if ((horizontalSum >= 40 && (horizontalSum - verticalSum >= 100)) || (horizontalSum >= 20 && verticalSum <= 5)) {
			horizontal = true;
		}
		if ((verticalSum >= 40 && (verticalSum - horizontalSum >= 100)) || (verticalSum >= 20 && horizontalSum <= 5)) {
			vertical = true;
		}

		// if non-white pixels were detected in both detection areas
		if ((vertical && horizontal) || !(vertical || horizontal)) {
			Object[] options = { "Vertical", "Horizontal", "Cancel" };
			dt.open(picture);
			int n = JOptionPane.showOptionDialog(null,
					"This PDF page's orientation could not be determined.\n"
							+ "Select your desired orientation below.",
					"PDF Orientation Error", JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, options, options[2]);

			if (n == JOptionPane.YES_OPTION) {
				orientation = 0;
			}
			if (n == JOptionPane.NO_OPTION) {
				orientation = 1;
			}
			if (n == JOptionPane.CANCEL_OPTION
					|| n == JOptionPane.CLOSED_OPTION) {
				System.exit(0);
			}
			Runtime rt = Runtime.getRuntime();
			rt.exec("TASKKILL /FI \"WINDOWTITLE eq my_image_*\"");
		} else if (vertical) {
			orientation = 0;
		} else if (horizontal) {
			orientation = 1;
		} else {
			JOptionPane.showMessageDialog(null,
					"An error occurred while trying to read one of the pages'\n"
							+ "orientation. The process will be cancelled.",
					"Error!", JOptionPane.OK_OPTION);
			System.exit(0);
		}
		return orientation;
	}

	public static void splitPDFFile(String fileName) {
		// splitting "documents" up by consecutive orientations
		try {
			int[] oriens = new int[pReader.getNumberOfPages()];
			ArrayList<Integer> splitSizes = new ArrayList<Integer>();
			int splitSize = 1;
			int position = 0;
			for (int i = 1; i <= pReader.getNumberOfPages(); i++) {
				String picName = System.getProperty("user.home")
						+ "\\Desktop\\Scan Folder\\Temp Pictures\\my_image_"
						+ i + ".jpg";
				oriens[i - 1] = getOrientation(new Picture(picName));
				System.out.print(oriens[i - 1]);
				if (i == 1) {
					splitSizes.add(1);
				} else if (oriens[i - 1] == oriens[i - 2]) {
					splitSize++;
					if (i == pReader.getNumberOfPages()) {
						splitSizes.set(position, splitSize);
					}
				} else if (oriens[i - 1] != oriens[i - 2]) {
					splitSizes.set(position, splitSize);
					position++;
					splitSizes.add(1);
					splitSize = 1;
				} else {
					System.out.println("Error");
				}

			}
			System.out.println("\n\n" + splitSizes);
			System.out.println("\nSuccessfully read input file: " + fileName + "\n");
			int totalPages = pReader.getNumberOfPages();
			System.out.println("There are total " + totalPages + " pages in this input file\n");
			split = 0;

			// Page numbers start from 1 to n; writing each set of pages to one file
			for (int pageNum = 1; pageNum <= totalPages; pageNum += splitSizes.get(split - 1)) {
				String outFile = System.getProperty("user.home")
						+ "\\Desktop\\Scan Folder\\Temp PDFs\\temp_" + split
						+ ".pdf";
				Document document = new Document(pReader.getPageSizeWithRotation(pageNum));
				PdfCopy writer = new PdfCopy(document, new FileOutputStream(outFile));
				document.open();
				int tempPageCount = 0;
				for (int pN = 0; pN < splitSizes.get(split); pN++) {
					PdfImportedPage page = writer.getImportedPage(pReader, pageNum + pN);
					writer.addPage(page);
					tempPageCount++;
				}

				System.out.println("pageNum: " + pageNum
						+ "     splitSizes.get(split): "
						+ splitSizes.get(split));

				document.close();
				/*
				 * The following will trigger the PDF file being written to the
				 * system
				 */

				writer.close();

				crop(outFile, fileName, split, verFlag);

				System.out.println("Split: [" + tempPageCount + " page]: "
						+ outFile);
				split++;
				verFlag = !verFlag; // alternating to determine the cropped orientation of the PDF
			}
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null,
					"There was an error while splitting up\n" + "the PDF.",
					"Error!", JOptionPane.OK_OPTION);
			System.exit(0);
		}
	}

	public static void crop(String inFileName, String outFileName, int split, boolean vert)
			throws Exception {
		
		PdfReader reader = new PdfReader(new File(inFileName).getAbsolutePath());
		String PDFName = outFileName.substring(outFileName.lastIndexOf("Folder\\") + 7, outFileName.indexOf(".pdf"));
		File fn = new File(scanFolder + PDFName + "\\" + PDFName + "_split_" + split + ".pdf");
		fn.getParentFile().mkdirs();
		
		int count = reader.getNumberOfPages();
		Document doc = new Document();
		PdfCopy copy = new PdfCopy(doc, new FileOutputStream(
				fn.getAbsolutePath()));
		doc.open();
		if (vert) {
			for (int i = 1; i <= count; i++) {
				reader.getPageN(i).put(PdfName.CROPBOX,
						new PdfRectangle(PageSize.LETTER));
				copy.addPage(copy.getImportedPage(reader, i));
			}
		} else {
			if (!doubSided) {
				for (int j = 1; j <= count; j++) {
					reader.getPageN(j).put(PdfName.CROPBOX,
							new PdfRectangle((new Rectangle(0, 180, 792, 792))));
					PdfDictionary pageDict;
					int rot = reader.getPageRotation(j);
					pageDict = reader.getPageN(j);
					pageDict.put(PdfName.ROTATE, new PdfNumber(rot + 90));
					copy.addPage(copy.getImportedPage(reader, j));
				}
			} else {
				for (int j = 1; j <= count; j++) {
					reader.getPageN(j).put(PdfName.CROPBOX,
							new PdfRectangle(new Rectangle(0, 180, 792, 792)));
					PdfDictionary pageDict;
					pageDict = reader.getPageN(j);
					if (j % 2 == 0) { // even
						pageDict.put(PdfName.ROTATE, new PdfNumber(270));
					} else { // odd
						pageDict.put(PdfName.ROTATE, new PdfNumber(90));
					}
					copy.addPage(copy.getImportedPage(reader, j));
				}
			}
		}
		doc.close();
	}
}
