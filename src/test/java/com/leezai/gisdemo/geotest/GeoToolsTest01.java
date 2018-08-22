package com.leezai.gisdemo.geotest;


import java.io.File;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class GeoToolsTest01 {


    /**
     * shape格式文件最少包含3个文件，他们的后缀是：.shp, .dbf, .shx
     .shp存储地理形状和位置信息
     .dbf存储属性信息
     .shx是索引文件
     */

    //1.读DBF文件
    @Test
    public void ReadDBF() throws Exception{

        String path = "F:\\BaiduNetdiskDownload\\gis.osm_buildings_a_free_1.dbf";

        //1.DbaseFileReader 用来读dbf文件
        DbaseFileReader reader = null;

        reader = new DbaseFileReader(new ShpFiles(path), true, Charset.forName("UTF-8"));

        //2.从文件中获取标题
        DbaseFileHeader header = reader.getHeader();

        //3.得到标题的字段【域】有多少个
        int numFileds = header.getNumFields();

        System.out.println("字段【列】的个数："+numFileds);

        //先输出列名
        for(int i=0;i<numFileds;i++){
            System.out.print(header.getFieldName(i)+"\t");
        }
        int stop = 0;
        //4.迭代读取记录 只读取前30条
        while(reader.hasNext()){
            if(stop>30){
                break;
            }
            //获取下一个记录【条目】
            Object[] entry = reader.readEntry();
            for(int i =0;i<numFileds;i++){
                Object value = entry[i];
                System.out.print(value+"\t");
            }//end for
            System.out.println();
            stop++;
        }//end while

        if(reader!=null){
            reader.close();
        }
        System.out.printf("-------总数量--------:"+stop);
    }


    //2.3个文件一起读，以Point为例
    @Test
    public void ReadSHP() throws Exception{

        //A.建筑物的shapefile，多边形 MULTIPOLYGON
        //String path = "F:\\BaiduNetdiskDownload\\gis.osm_buildings_a_free_1.shp";

        //B.路的shapefile，多线MULTILINESTRING
        //String path = "E:\\china-latest-free\\gis.osm_roads_free_1.shp";

        //C.建筑物的点坐标  以Point为主
        String path = "F:\\BaiduNetdiskDownload\\gis.osm_buildings_a_free_1.shp";

        //一个数据存储实现，允许从Shapefiles读取和写入
        ShapefileDataStore shpDataStore = null;
        shpDataStore = new ShapefileDataStore(new File(path).toURI().toURL());
        shpDataStore.setCharset(Charset.forName("UTF-8"));
        //获取这个数据存储保存的类型名称数组
        //getTypeNames:获取所有地理图层
        String typeName = shpDataStore.getTypeNames()[0];
        //通过此接口可以引用单个shapefile、数据库表等。与数据存储进行比较和约束
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = null;
        featureSource = (FeatureSource<SimpleFeatureType, SimpleFeature>)shpDataStore.getFeatureSource(typeName);
        //一个用于处理FeatureCollection的实用工具类。提供一个获取FeatureCollection实例的机制
        FeatureCollection<SimpleFeatureType, SimpleFeature> result=featureSource.getFeatures();

        System.out.println("-----结果总量------:"+result.size());

        FeatureIterator<SimpleFeature> iterator = result.features();
        //迭代 特征  只迭代30个 太大了，一下子迭代完，非常耗时
        int stop = 0;
        while(iterator.hasNext()){

            if(stop >30){
                break;
            }

            SimpleFeature feature = iterator.next();
            Collection<Property> p = feature.getProperties();
            Iterator<Property> it  = p.iterator();

            //特征里面的属性再迭代,属性里面有字段
            System.out.println("================================");
            while(it.hasNext()){
                Property pro = it.next();
                //如果是点的话,基本上第一个属性字段表示的就是类型
                if(pro.getValue() instanceof Point){

                    Point point = (Point)pro.getValue();

                    System.out.println("【位置】PointX = "+point.getX()+",PoinxY = "+point.getY());

                }
                //其余的，正常输出
                else{
                    System.out.println(pro.getName()+"\t = "+pro.getValue());
                }
            }//end 里层while

            stop++;
        }//end 最外层 while

        iterator.close();
    }

    //3.写shp文件，：
    @Test
    public void WriteSHP() throws Exception{

        String path="C:\\my.shp";

        //1.创建shape文件对象
        File file =new File(path);

        Map<String, Serializable> params = new HashMap<>();

        //用于捕获参数需求的数据类
        //URLP:url to the .shp file.
        params.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL());

        //2.创建一个新的数据存储——对于一个还不存在的文件。
        ShapefileDataStore ds = (ShapefileDataStore) new ShapefileDataStoreFactory().createNewDataStore(params);

        //3.定义图形信息和属性信息
        //SimpleFeatureTypeBuilder 构造简单特性类型的构造器
        SimpleFeatureTypeBuilder tBuilder = new SimpleFeatureTypeBuilder();

        //设置
        //WGS84:一个二维地理坐标参考系统，使用WGS84数据
        tBuilder.setCRS(DefaultGeographicCRS.WGS84);
        tBuilder.setName("shapefile");

        //添加 一个点
        tBuilder.add("the_geom", Point.class);
        //添加一个id
        tBuilder.add("osm_id", Long.class);
        //添加名称
        tBuilder.add("name", String.class);

        //添加描述
        tBuilder.add("des", String.class);

        //设置此数据存储的特征类型
        ds.createSchema(tBuilder.buildFeatureType());

        //设置编码
        ds.setCharset(Charset.forName("UTF-8"));

        //设置writer
        //为给定的类型名称创建一个特性写入器

        //1.typeName：特征类型
        //2.transaction :事物,写入失败，回滚
        //3.ShapefileDataStore::getTypeNames:
		/*public String[] getTypeNames()
		 获取这个数据存储保存的类型名称数组。
		ShapefileDataStore总是返回一个名称
		*/
        FeatureWriter<SimpleFeatureType, SimpleFeature> writer = ds.getFeatureWriter(
                ds.getTypeNames()[0], Transaction.AUTO_COMMIT);



        //Interface SimpleFeature：一个由固定列表值以已知顺序组成的SimpleFeatureType实例。
        //写一个点
        SimpleFeature feature = writer.next();

        //SimpleFeature ::setAttribute(String attrName, Object val)
        //给指定的属性名称添加一个对象 POINT

        double x = 116.123; //X轴坐标
        double y = 39.345 ; //Y轴坐标

		/*
		 * Coordinate : GeoAPI几何接口的实现
		 一个轻量级的类，用于存储二维笛卡尔平面上的坐标。
		 它不同于点，它是几何的一个子类。
		 不同于类型点的对象(包含额外的信息，如信封、精确模型和空间引用系统信息)，
		 坐标只包含有序值和访问方法。
		 */
        Coordinate coordinate = new Coordinate(x, y);

        //GeometryFactory:提供一套实用的方法，用于从坐标列表中构建几何对象。
        //构造一个几何图形工厂，生成具有浮动精度模型的几何图形和一个0的空间引用ID。
        Point point = new GeometryFactory().createPoint(coordinate);

        feature.setAttribute("the_geom",point);
        feature.setAttribute("osm_id", 1234567890l);
        feature.setAttribute("name", "帅鱼");
        feature.setAttribute("des", "爱宝宝");


        //再来一个点

        feature = writer.next();

        x = 116.456;
        y = 39.678 ;
        coordinate = new Coordinate(x, y);
        point = new GeometryFactory().createPoint(coordinate);

        feature.setAttribute("the_geom",point);
        feature.setAttribute("osm_id", 1234567891l);
        feature.setAttribute("name", "宝宝");
        feature.setAttribute("des", "爱帅鱼");

        //写入
        writer.write();

        //关闭
        writer.close();

        //释放资源
        ds.dispose();


        //读取shapefile文件的图形信息
        ShpFiles shpFiles = new ShpFiles(path);
		/*ShapefileReader(
		 ShpFiles shapefileFiles,
		 boolean strict, --是否是严格的、精确的
		 boolean useMemoryMapped,--是否使用内存映射
		 GeometryFactory gf,     --几何图形工厂
		 boolean onlyRandomAccess--是否只随机存取
		 )
		*/
        ShapefileReader reader = new ShapefileReader(shpFiles,
                false, true, new GeometryFactory(), false);
        while(reader.hasNext()){
            System.out.println(reader.nextRecord().shape());
        }

        reader.close();


    }


    //4.读shp文件【几何信息+属性信息】
    @Test
    public void SHPRead() throws Exception{
        //基于上面新建的shapfile文件，进行读取
        //String path = "C:\\my.shp";
        String path = "F:\\BaiduNetdiskDownload\\gis.osm_buildings_a_free_1.shp";
        //构建shapefile数据存储的实例
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

        //基于路径构建文件对象
        File file = new File(path);

        //构建一个已存在的shapfile数据源
        //ShapefileDataStore:数据存储实现，允许从Shapefiles读取和写入
        ShapefileDataStore ds = (ShapefileDataStore) dataStoreFactory.createDataStore(file.toURI().toURL());

        //设置编码，防止中文读取乱码
        ds.setCharset(Charset.forName("UTF-8"));

        //getFeatureSource():ContentFeatureSource
        //这个特性是由 FeatureCollection提供的操作完成的。单独的特征记忆实现由子类提供:
        //SimpleFeatureSource特征资源明确地使用FeatureCollection【集合】，可迭代
        SimpleFeatureSource featureSource = ds.getFeatureSource();

        //getFeatures():以FeatureCollection的形式检索所有特性。
        //一个用于处理FeatureCollection的实用工具类。提供一个获取FeatureCollection实例的机制
        FeatureCollection<SimpleFeatureType, SimpleFeature> result=featureSource.getFeatures();

        System.out.println("几何对象总过有："+result.size());
        //features():返回一个FeatureIterator迭代器
        SimpleFeatureIterator it =(SimpleFeatureIterator) result.features();


        while(it.hasNext()){
            SimpleFeature feature = it.next();
            //迭代属性【属性我们可以理解为一个几何对象的属性节点，也就是对一个几何图形的描述字段】
            Iterator<Property> ip = feature.getProperties().iterator();
            System.out.println("========================");
            //再来个while
            while(ip.hasNext()){
                Property pro = ip.next();
                System.out.println(pro.getName()+" = "+pro.getValue());
            }//end 属性迭代
        }

        it.close();
    }

}

