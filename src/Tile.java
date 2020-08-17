import java.awt.image.BufferedImage;

public  class Tile {
    public static final int TILE_WIDTH = 20;
    public static final int TILE_HEIGHT = 20;
    public static final int TILE_SCALE = 1;

    public static int SCALED_WIDTH = TILE_WIDTH / TILE_SCALE;
    public static int SCALED_HEIGHT = TILE_HEIGHT / TILE_SCALE;
    public Pixel[][] pixels = new Pixel[SCALED_WIDTH][SCALED_HEIGHT];
    public BufferedImage image;

    public Tile(BufferedImage image) {
        this.image = image;
        calcPixels();
    }

    private void calcPixels(){
        for(int x=0; x<SCALED_WIDTH; x++){
            for(int y=0; y<SCALED_HEIGHT; y++){
                pixels[x][y] = calcPixel(x * TILE_SCALE, y * TILE_SCALE, TILE_SCALE, TILE_SCALE);
            }
        }
    }

    private Pixel calcPixel(int x, int y, int w, int h){
        int redTotal = 0, greenTotal = 0, blueTotal = 0;

        for(int i=0; i<w; i++){
            for(int j=0; j<h; j++){
                int rgb = image.getRGB(x+i, y+j);
                redTotal   += Mosaic.getRed(rgb);
                greenTotal += Mosaic.getGreen(rgb);
                blueTotal  += Mosaic.getBlue(rgb);
            }
        }
        int count = w*h;
        return new Pixel(redTotal/count, greenTotal/count, blueTotal/count);
    }
}