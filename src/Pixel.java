public  class Pixel{
    public int r,g,b;

    public Pixel(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }
    @Override
    public String toString() {
        return r + "." + g + "." + b;
    }
}