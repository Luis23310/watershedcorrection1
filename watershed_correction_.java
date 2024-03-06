import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.*;
import ij.plugin.filter.ParticleAnalyzer;

public class watershed_correction_ implements PlugIn {
    public void run(String arg) {
        // Check if the user clicked on "About"
        if (arg.equals("about")) {
            showAbout();
            return;
        }

        // Get the IDs of open images
        int[] wList = WindowManager.getIDList();
        if (wList == null || wList.length < 2) {
            IJ.showMessage("Error", "This plugin requires at least two images to be open.");
            return;
        }

        // Get the titles of open images
        String[] titles = new String[wList.length];
        for (int i = 0; i < wList.length; i++) {
            ImagePlus imp = WindowManager.getImage(wList[i]);
            titles[i] = imp != null ? imp.getTitle() : "";
        }

        // Display a dialog for selecting images to compare
        GenericDialog gd = new GenericDialog("Select Images to Compare");
        gd.addChoice("Image 1:", titles, titles[0]);
        gd.addChoice("Image 2:", titles, titles[1]);
        gd.showDialog();
        if (gd.wasCanceled())
            return;

        // Get the selected images
        int index1 = gd.getNextChoiceIndex();
        ImagePlus imp1 = WindowManager.getImage(wList[index1]);
        int index2 = gd.getNextChoiceIndex();
        ImagePlus imp2 = WindowManager.getImage(wList[index2]);

        // Check if the images are 16-bit grayscale
        if (imp1.getType() != ImagePlus.GRAY16 || imp2.getType() != ImagePlus.GRAY16) {
            IJ.error("Error", "Both images must be 16-bit grayscale.");
            return;
        }

        // Get the processors of the images
        ShortProcessor ip1 = (ShortProcessor) imp1.getProcessor();
        ShortProcessor ip2 = (ShortProcessor) imp2.getProcessor();
        int width = ip1.getWidth();
        int height = ip1.getHeight();

        // Check if the images have the same dimensions
        if (width != ip2.getWidth() || height != ip2.getHeight()) {
            IJ.error("Error", "Images must have the same dimensions.");
            return;
        }

        // Create a new image to display the differences
        ImageProcessor diffIp = new ShortProcessor(width, height);

        // Calculate absolute difference between pixel values
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int diff = Math.abs(ip1.getPixel(x, y) - ip2.getPixel(x, y));
                diffIp.putPixelValue(x, y, diff);
            }
        }

        // Create a new ImagePlus to display the differences
        ImagePlus diffImage = new ImagePlus("Difference Image", diffIp);
        diffImage.show();
        diffImage.updateAndDraw();

        // Prompt the user to select another image
        gd = new GenericDialog("Select Additional Image");
        gd.addChoice("Additional Image:", titles, titles[0]);
        gd.showDialog();
        if (gd.wasCanceled())
            return;

        // Get the selected additional image
        int index3 = gd.getNextChoiceIndex();
        ImagePlus imp3 = WindowManager.getImage(wList[index3]);
        if (imp3 != null) {
            imp3.show();
            
            // Obtener el ImageProcessor
            ImageProcessor ip = imp3.getProcessor();

            // Obtener el tamaño de la imagen
            int imgWidth = ip.getWidth();
            int imgHeight = ip.getHeight();

            // Inicializar variables para el área y el perímetro
            double area = 0.0;
            double perimeter = 0.0;
            
            // Create ResultsTable to store the data
            ResultsTable rt = new ResultsTable();
            
            // Iterar sobre la imagen y calcular el área y el perímetro de los granos de arroz
            for (int y = 0; y < imgHeight; y++) {
                for (int x = 0; x < imgWidth; x++) {
                    if (ip.getPixel(x, y) == 255) { // Si el píxel es blanco
                        area++; // Incrementar el área
                        perimeter += countNeighbors(ip, x, y); // Sumar los vecinos
                    }
                }
            }
            
            // Add the results to the ResultsTable
            rt.incrementCounter();
            rt.addValue("Area", area);
            rt.addValue("Perimeter", perimeter);
            
            // Show the ResultsTable
            rt.show("Results");
        } else {
            IJ.showMessage("Error", "Could not open the selected image.");
        }
    }

    // Método para contar los vecinos de un píxel blanco
    private static int countNeighbors(ImageProcessor ip, int x, int y) {
        int count = 0;
        int[][] neighbors = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // Vecinos: izquierda, derecha, arriba, abajo
        for (int[] neighbor : neighbors) {
            int nx = x + neighbor[0];
            int ny = y + neighbor[1];
            if (nx >= 0 && nx < ip.getWidth() && ny >= 0 && ny < ip.getHeight() && ip.getPixel(nx, ny) == 255) {
                count++;
            }
        }
        return count;
    }

   

    void showAbout() {
        IJ.showMessage("About Rice_Grain_Comparator...", "This plugin compares two 16-bit grayscale images and displays the absolute difference between them.");
    }
}
