package pl.ninecube.slicer;

import org.apache.poi.ss.usermodel.Cell;
import pl.ninecube.slicer.annotation.ExcelName;
import pl.ninecube.slicer.annotation.ExcelTab;
import pl.ninecube.slicer.excell.ExcelReader;
import pl.ninecube.slicer.excell.exception.ErrorOpenExcellFileException;
import pl.ninecube.slicer.excell.exception.ExcelException;

import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Slicer {

    private Path excelPath;

    public Slicer fromExcel(Path path) {
        this.excelPath = path;
        return this;
    }

    public <T> Map<String, T> map(Class<T> modelClass) throws FileNotFoundException, ErrorOpenExcellFileException {

        Map<String, Object> valuesTree = new HashMap<>();
        valuesTree.put(excelPath.getFileName().toString(), processExcel(excelPath));

        return mapClass(valuesTree, modelClass);
    }

    private <T> Map<String, T> mapClass(Map<String, Object> valuesTree, Class<T> modelClass) {

        Map<String, T> mapedClass = new HashMap<>();

        for (String fileName : valuesTree.keySet()) {
            HashMap<String, Object> o1 = (HashMap<String, Object>) valuesTree.get(fileName);


            ExcelTab excelTab = modelClass.getAnnotation(ExcelTab.class);

            HashMap<String, Object> o2 = (HashMap<String, Object>) o1.get(excelTab.value());

            for (String id : o2.keySet()) {

                HashMap<String, Object> o3 = (HashMap<String, Object>) o2.get(id);

                T model = fillModel(o1, id, modelClass, o3);

                mapedClass.put(id, model);
            }
        }

        return mapedClass;
    }

    private <T> T fillModel(HashMap<String, Object> o1, String id, Class<T> modelClass, HashMap<String, Object> o3) {
        T object = instanceModel(modelClass);

        for (Field field : modelClass.getDeclaredFields()) {

            System.out.println("Processing " + field.getName());

            String fieldName = field.getName();

            ExcelName excelName = field.getAnnotation(ExcelName.class);
            if (excelName != null) {
                fieldName = excelName.value();
            }

            boolean isAnotherSheet = false;

            ExcelTab excelTab = field.getAnnotation(ExcelTab.class);
            if (excelTab != null) {
                o3 = (HashMap<String, Object>) ((HashMap<String, Object>) o1.get(excelTab.value())).get(id);
                isAnotherSheet = true;
            }

            field.setAccessible(true);
            try {
                Object val = null;
                if (isBasicType(field.getType())) {
                    Cell o = (Cell) o3.get(fieldName);

                    if (field.getType().getSimpleName().equals("boolean") || field.getType().getSimpleName().equals("Boolean")) {
                        try{
                            val = o.getBooleanCellValue();
                        }catch (IllegalStateException e){
                            val = !o.getStringCellValue().isEmpty();
                        }

                    }
                    if (field.getType().getSimpleName().equals("float") || field.getType().getSimpleName().equals("Float")) {
                        try{
                            val = (float) o.getNumericCellValue();
                        }catch (IllegalStateException e){
                            val = Float.parseFloat(o.getStringCellValue());
                        }

                    }
                    if (field.getType().getSimpleName().equals("double") || field.getType().getSimpleName().equals("Double")) {
                        try{
                            val = (double) o.getNumericCellValue();
                        }catch (IllegalStateException e){
                            val = Double.parseDouble(o.getStringCellValue());
                        }

                    }
                    if (field.getType().getSimpleName().equals("int") || field.getType().getSimpleName().equals("Integer")) {
                        try{
                            val = (int) o.getNumericCellValue();
                        }catch (IllegalStateException e){
                            val = Integer.parseInt(o.getStringCellValue());
                        }

                    }
                    if (field.getType().getSimpleName().equals("String")) {
                        val = o.getStringCellValue();
                    }

                } else {

                    if (isAnotherSheet) {
                        val = fillModel(o1, id, field.getType(), o3);
                    } else {
                        HashMap<String, Object> o4 = (HashMap<String, Object>) o3.get(fieldName);
                        val = fillModel(o1, id, field.getType(), o4);
                    }
                }

                field.set(object, val);
                field.setAccessible(false);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return object;
    }

    private <T> T instanceModel(Class<T> modelClass) {
        T object = null;
        try {
            object = modelClass.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return object;
    }

    private boolean isBasicType(Class<?> type) {
        return type.getSimpleName().equals("boolean") ||
                type.getSimpleName().equals("float") ||
                type.getSimpleName().equals("double") ||
                type.getSimpleName().equals("int") ||
                type.getSimpleName().equals("Boolean") ||
                type.getSimpleName().equals("Float") ||
                type.getSimpleName().equals("Double") ||
                type.getSimpleName().equals("Integer") ||
                type.getSimpleName().equals("String");

    }

    private Map<String, Object> processExcel(Path excelPath) throws FileNotFoundException, ErrorOpenExcellFileException {
        ExcelReader.openFile(excelPath);
        Map<String, List<Cell>> idsPerSheet = readIdsFromSheets();
        Map<String, List<Cell>> keysPerSheet = readKeysFromSheets();

        return createValueTree(idsPerSheet, keysPerSheet);
    }

    private Map<String, Object> createValueTree(Map<String, List<Cell>> idsPerSheet, Map<String, List<Cell>> keysPerSheet) {

        Map<String, Object> map = new HashMap<>();

        for (String sheet : idsPerSheet.keySet()) {

            try {
                ExcelReader.readFromSheet(sheet);
            } catch (ExcelException e) {
                e.printStackTrace();
            }

            List<Cell> idCellList = idsPerSheet.get(sheet);

            Map<String, Object> structurePerId = new HashMap<>();
            for (Cell idCell : idCellList) {
                List<Cell> keysCellList = keysPerSheet.get(sheet);

                structurePerId = StructIterator.parseToTree(idCellList, keysCellList);

//                parseKeys(structurePerId, idCell, keysCellList);
            }
            map.put(sheet, structurePerId);
        }


        return map;
    }


    private Map<String, List<Cell>> readIdsFromSheets() {
        Map<String, List<Cell>> map = new HashMap<>();

        List<String> allSheets = ExcelReader.getAllSheets();

        for (String sheet : allSheets) {
            readFromSheet(sheet);
            map.put(sheet, readAllIds());
        }

        return map;
    }

    private Map<String, List<Cell>> readKeysFromSheets() {
        Map<String, List<Cell>> map = new HashMap<>();

        List<String> allSheets = ExcelReader.getAllSheets();

        for (String sheet : allSheets) {
            readFromSheet(sheet);
            map.put(sheet, readAllKeys());
        }

        return map;
    }

    private List<Cell> readAllKeys() {
        List<Cell> keyList = new ArrayList<>();

        int x = 1;

        do {
            try {
                Cell cell = ExcelReader.readCell(0, x++);
                if (!cell.getStringCellValue().isEmpty())
                    keyList.add(cell);
            } catch (ExcelException e) {
                break;
            }
        } while (true);


        return keyList;
    }

    private List<Cell> readAllIds() {
        List<Cell> idList = new ArrayList<>();

        int y = 1;

        do {
            try {
                Cell cell = ExcelReader.readCell(y++, 0);
                if (!cell.getStringCellValue().isEmpty())
                    idList.add(cell);
            } catch (ExcelException e) {
                break;
            }
        } while (true);

        return idList;
    }

    private void readFromSheet(String sheet) {
        try {
            ExcelReader.readFromSheet(sheet);
        } catch (ExcelException e) {
            e.printStackTrace();
        }
    }
}
