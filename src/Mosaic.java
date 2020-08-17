
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

public class Mosaic {
    private static final String TILES_DIR = "C:\\Users\\Alien17\\Downloads\\caltech-101\\101_ObjectCategories\\airplanes";
    private static final String INPUT_IMG = "C:\\Users\\Alien17\\Pictures\\fireeel.jpg";
    private static final String OUTPUT_IMG = "C:\\Users\\Alien17\\Pictures\\finalimage.jpg";

    private static final boolean IS_BW = false;
    private static final int THREADS = 2;

    private static void log(String msg){
        System.out.println(msg);
    }

    public static void main(String[] args) throws IOException, InterruptedException{

        log("Reading tiles...");
        final Collection<Tile> tileImages = getImagesFromTiles(new File(TILES_DIR));

        log("Processing input image...");
        File inputImageFile = new File(INPUT_IMG);
        Collection<BufferedImagePart> inputImageParts = getImagesFromInput(inputImageFile);
        final Collection<BufferedImagePart> outputImageParts = Collections.synchronizedSet(new HashSet<BufferedImagePart>());

        ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(THREADS);

        final AtomicInteger i = new AtomicInteger();
        final int partCount = inputImageParts.size();
        for (final BufferedImagePart inputImagePart : inputImageParts) {
            newFixedThreadPool.execute(new Runnable(){
                public void run() {
                    Tile bestFitTile = getBestFitTile(inputImagePart.image, tileImages);
                    log(String.format("Matching part %s of %s", i.incrementAndGet(), partCount));
                    outputImageParts.add(new BufferedImagePart(bestFitTile.image, inputImagePart.x, inputImagePart.y));
                }
            });
        }

        newFixedThreadPool.shutdown();
        newFixedThreadPool.awaitTermination(10000000, TimeUnit.SECONDS);

        log("Writing output image...");
        BufferedImage inputImage = ImageIO.read(inputImageFile);
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        BufferedImage output = makeOutputImage(width, height, outputImageParts);
        ImageIO.write(output, "png", new File(OUTPUT_IMG));
        log("FINISHED");
    }

    private static BufferedImage makeOutputImage(int width, int height, Collection<BufferedImagePart> parts){
        BufferedImage image = new BufferedImage(width * Tile.TILE_SCALE, height * Tile.TILE_SCALE, BufferedImage.TYPE_3BYTE_BGR);

        for(BufferedImagePart part : parts){
            BufferedImage imagePart = image.getSubimage(part.x * Tile.TILE_SCALE, part.y * Tile.TILE_SCALE, Tile.TILE_WIDTH, Tile.TILE_HEIGHT);
            imagePart.setData(part.image.getData());
        }

        return image;
    }

    private static Tile getBestFitTile(BufferedImage target, Collection<Tile> tiles) {
        Tile bestFit = null;
        int bestFitScore = -1;

        for(Tile tile : tiles){
            int score = getScore(target, tile);
            if (score > bestFitScore){
                bestFitScore = score;
                bestFit = tile;
            }
        }

        return bestFit;
    }

    private static int getScore(BufferedImage target, Tile tile){
        assert target.getHeight() == Tile.SCALED_HEIGHT;
        assert target.getWidth() == Tile.SCALED_WIDTH;

        int total = 0;
        for(int x = 0; x<Tile.SCALED_WIDTH; x++){
            for(int y = 0; y<Tile.SCALED_HEIGHT; y++){
                int targetPixel = target.getRGB(x, y);
                Pixel candidatePixel = tile.pixels[x][y];
                int diff = getDiff(targetPixel, candidatePixel);
                int score;
                if (IS_BW){
                    score = 255 - diff;
                } else {
                    score = 255 * 3 - diff;
                }

                total += score;
            }
        }

        return total;
    }

    private static int getDiff(int target, Pixel candidate){
        if (IS_BW){
            return Math.abs(getRed(target) - candidate.r);
        } else {
            return Math.abs(getRed(target) - candidate.r) +
                    Math.abs(getGreen(target) - candidate.g) +
                    Math.abs(getBlue(target) - candidate.b);
        }
    }

    static int getRed(int pixel){
        return  (pixel >>> 16) & 0xff;
    }

    static int getGreen(int pixel){
        return  (pixel >>> 8) & 0xff;
    }

    static int getBlue(int pixel){
        return  pixel & 0xff;
    }

    private static Collection<Tile> getImagesFromTiles(File tilesDir) throws IOException{
        Collection<Tile> tileImages = Collections.synchronizedSet(new HashSet<Tile>());
        File[] files = tilesDir.listFiles();
        for(File file : files){
            BufferedImage img = ImageIO.read(file);
            if (img != null){
                tileImages.add(new Tile(img));
            } else {
                System.err.println("null image for file " + file.getName());
            }
        }
        return tileImages;
    }

    private static Collection<BufferedImagePart> getImagesFromInput(File inputImgFile) throws IOException{
        Collection<BufferedImagePart> parts = new HashSet<BufferedImagePart>();

        BufferedImage inputImage = ImageIO.read(inputImgFile);
        int totalHeight = inputImage.getHeight();
        int totalWidth = inputImage.getWidth();

        int x=0, y=0, w=Tile.SCALED_WIDTH, h=Tile.SCALED_HEIGHT;
        while(x+w <= totalWidth){
            while(y+h <= totalHeight){
                BufferedImage inputImagePart = inputImage.getSubimage(x, y, w, h);
                parts.add(new BufferedImagePart(inputImagePart, x, y));
                y+=h;
            }
            y=0;
            x+= w;
        }

        return parts;
    }
}
