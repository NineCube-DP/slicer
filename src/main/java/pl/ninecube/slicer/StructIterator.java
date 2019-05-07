package pl.ninecube.slicer;

import org.apache.poi.ss.usermodel.Cell;
import pl.ninecube.slicer.excell.ExcelReader;
import pl.ninecube.slicer.excell.exception.ExcelException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StructIterator {

    private static List<String> lists;
    private static List<String> structs;

    public static Map<String, Object> parseToTree(List<Cell> idCellList, List<Cell> keysCellList) {
        initIterator();

        Map<String, Object> tree = new HashMap<>();

        for (Cell idCell : idCellList) {
            tree.put(idCell.getStringCellValue(), parse(idCell, keysCellList));
        }

        return tree;
    }

    private static Map<String, Object> parse(Cell idCell, List<Cell> keysCellList) {

        Map<String, Object> map = new HashMap<>();

        Map<String, Object> basicKeys = identifyBasicKeys(idCell, keysCellList);
        Map<String, Object> structs = identifyStructs(idCell, keysCellList);
        Map<String, Object> lists = identifyLists(idCell, keysCellList);


        map.putAll(basicKeys);
        map.putAll(structs);
        map.putAll(lists);

        return map;
    }

    private static Map<String, Object> identifyLists(Cell idCell, List<Cell> keysCellList) {
        String listName = "";
        int startListIndex = 0;

        Map<String, List<Cell>> lists = new HashMap<>();

        for (Cell keyCell : keysCellList) {

            if (isListKey(keyCell) && listName.isEmpty()) {
                startListIndex = keysCellList.indexOf(keyCell);
                listName = normalizeKey(keyCell.getStringCellValue());

            } else if (isEndListKey(keyCell)) {
                if (normalizeKey(keyCell.getStringCellValue()).equals(listName)) {
                    lists.put(listName, keysCellList.subList(startListIndex + 1, keysCellList.indexOf(keyCell)));
                    listName = "";
                }
            }
        }

        Map<String, Object> parsedLists = new HashMap<>();
        for (String structNameKey : lists.keySet()) {
            parsedLists.put(structNameKey, parse(idCell, lists.get(structNameKey)));
        }


        return parsedLists;
    }

    private static Map<String, Object> identifyStructs(Cell idCell, List<Cell> keysCellList) {

        String structName = "";
        int startStructIndex = 0;

        Map<String, List<Cell>> structs = new HashMap<>();

        for (Cell keyCell : keysCellList) {

            if (isListKey(keyCell)) {
                openList(keyCell);
            } else if (isEndListKey(keyCell)) {
                closeList(keyCell);
            } else if (isStructKey(keyCell) && structName.isEmpty()) {
                startStructIndex = keysCellList.indexOf(keyCell);
                structName = normalizeKey(keyCell.getStringCellValue());

            } else if (isEndStructKey(keyCell) && isAllListClose()) {
                if (normalizeKey(keyCell.getStringCellValue()).equals(structName)) {
                    structs.put(structName, keysCellList.subList(startStructIndex + 1, keysCellList.indexOf(keyCell)));
                    structName = "";
                }
            }
        }

        Map<String, Object> parsedStructs = new HashMap<>();
        for (String structNameKey : structs.keySet()) {
            parsedStructs.put(structNameKey, parse(idCell, structs.get(structNameKey)));
        }


        return parsedStructs;
    }

    private static Map<String, Object> identifyBasicKeys(Cell idCell, List<Cell> keysCellList) {
        Map<String, Object> keyValueMap = new HashMap<>();

        for (Cell keyCell : keysCellList) {

            if (isStructKey(keyCell)) {
                openStruct(keyCell);
            } else if (isEndStructKey(keyCell)) {
                closeStruct(keyCell);
            } else if (isListKey(keyCell)) {
                openList(keyCell);
            } else if (isEndListKey(keyCell)) {
                closeList(keyCell);
            } else if (isAllStructClose() && isAllListClose()) {
                try {
                    keyValueMap.put(keyCell.getStringCellValue().toLowerCase(), ExcelReader.readCell(idCell.getColumnIndex(), keyCell.getRowIndex()));
                } catch (ExcelException e) {
                    e.printStackTrace();
                }
            }
        }

        return keyValueMap;
    }

    private static void initIterator() {
        lists = new ArrayList<>();
        structs = new ArrayList<>();
    }

    private static boolean isAllListClose() {
        return lists.isEmpty();
    }


    private static void openList(Cell keyCell) {
        lists.add(keyCell.getStringCellValue().toLowerCase().replace("&", "").trim());
    }

    private static void closeList(Cell keyCell) {
        lists.remove(keyCell.getStringCellValue().toLowerCase().replace("!&", "").trim());
    }

    private static boolean isEndListKey(Cell keyCell) {
        return keyCell.getStringCellValue().startsWith("!&");
    }

    private static boolean isListKey(Cell keyCell) {
        return keyCell.getStringCellValue().startsWith("&");
    }

    private static void closeStruct(Cell keyCell) {
        structs.remove(keyCell.getStringCellValue().toLowerCase().replace("!#", "").trim());
    }

    private static boolean isEndStructKey(Cell keyCell) {
        return keyCell.getStringCellValue().startsWith("!#");
    }

    private static boolean isAllStructClose() {
        return structs.isEmpty();
    }

    private static String normalizeKey(String key) {
        return key
                .toLowerCase()
                .replace("#", "")
                .replace("&", "")
                .replace("!", "")
                .trim();
    }

    private static void openStruct(Cell keyCell) {
        structs.add(keyCell.getStringCellValue().toLowerCase().replace("#", "").trim());
    }

    private static boolean isStructKey(Cell keyCell) {
        return keyCell.getStringCellValue().startsWith("#");
    }
}
