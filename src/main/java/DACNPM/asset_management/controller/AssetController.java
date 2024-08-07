package DACNPM.asset_management.controller;

import DACNPM.asset_management.model.*;
import DACNPM.asset_management.service.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;


@Controller
public class AssetController {

    @Autowired
    AssetService assetService;

    @Resource
    StatusService statusService;
    @Resource
    TypeService typeService;

    @Resource
    WarehouseService warehouseService;

    @Resource
    CommentService commentService;

    @GetMapping("/home")
    public String getAllAsset(@SessionAttribute(name = "loggedInAccount", required = false) Account loggedInAccount, Model model, @Param("keyword") String keyword, HttpSession session) {
        if (loggedInAccount == null) {
            return "redirect:/login";
        }

        List<Asset> listAsset = assetService.getAllAssets(keyword);
        Map<Integer, Warehouse> warehouseMap = new HashMap<>();

        Map<Integer, Status> statusMap = new HashMap<>();
        Map<Integer, Type> typeMap = new HashMap<>();

        for (Asset asset : listAsset) {
            Optional<Warehouse> warehouseOptional = warehouseService.getQuantityByAssetId(asset.getIdAsset());
            if (warehouseOptional.isPresent()) {
                Warehouse warehouse = warehouseOptional.get();
                warehouseMap.put(asset.getIdAsset(), warehouse); // Store warehouse info by assetId
            }

            Optional<Status> statusOptional = assetService.getStatusByAssetId(asset.getIdAsset());
            if (statusOptional.isPresent()) {
                Status status = statusOptional.get();
                statusMap.put(asset.getIdAsset(), status);
            }

            Optional<Type> typeOptional = assetService.getTypeByAssetId(asset.getIdAsset());
            if (typeOptional.isPresent()) {
                Type type = typeOptional.get();
                typeMap.put(asset.getIdAsset(), type);
            }
        }

        List<Type> listType = typeService.getAllType();
        model.addAttribute("assetWarehouseMap", warehouseMap);
        model.addAttribute("statusMap", statusMap);
        model.addAttribute("typeMap", typeMap);
        model.addAttribute("listAsset", listAsset);
        model.addAttribute("listType", listType);
        model.addAttribute("asset", new Asset());
        model.addAttribute("loggedInAccount", loggedInAccount);
        model.addAttribute("keyword", keyword);

        return "index";
    }

    @ResponseBody
    @GetMapping("/listAsset")
    public List<Asset> getAll(@Param("keyword") String keyword) {
        return assetService.getAllAssets(keyword);
    }

    @PostMapping("/addNewAsset")
    public String addNewAsset(@ModelAttribute("asset") Asset asset, @RequestParam("quantity") int quantity) throws Exception {
        asset.setStatus(0);
        assetService.addNewAsset(asset);

        warehouseService.initWarehouse(asset.getIdAsset(), quantity, 0);
        return "redirect:/home";
    }

    @GetMapping("/updateAsset/{id}")
    public String getUpdateAsset(@SessionAttribute(name = "loggedInAccount", required = false) Account loggedInAccount, @PathVariable("id") int id, Model model) {

        if (loggedInAccount == null) {
            return "redirect:/login";
        }
        model.addAttribute("loggedInAccount", loggedInAccount);
        Optional<Asset> optionalAsset = assetService.findAssetById(id);
        Type oldType = typeService.getTypeByAssetId(id);
        List<Type> listType = typeService.getAllType();
        List<Status> listStatus = statusService.getAllStatus();
        Status oldStatus = statusService.getStatusByAssetId(id);
        Optional<Warehouse> warehouseOptional = warehouseService.getQuantityByAssetId(id);
        if (warehouseOptional.isPresent()) {
            Warehouse warehouse = warehouseOptional.get();
            model.addAttribute("warehouse", warehouse);
        }

        model.addAttribute("listType", listType);
        model.addAttribute("oldType", oldType);

        if (optionalAsset.isPresent()) {
            Asset asset = optionalAsset.get();
            model.addAttribute("asset", asset);
        } else {
            model.addAttribute("errorMessage", "Not found asset with ID: " + id);
        }

        model.addAttribute("listStatus", listStatus);
        model.addAttribute("oldStatus", oldStatus);
        return "edit-asset";
    }

    @GetMapping("/detailAsset/{id}")
    public String getDetailAsset(@SessionAttribute(name = "loggedInAccount", required = false) Account loggedInAccount, @PathVariable("id") int id, Model model) {
        Map<Integer, Status> statusMap = new HashMap<>();
        Map<Integer, Type> typeMap = new HashMap<>();
        Map<Integer, Warehouse> warehouseMap = new HashMap<>();
        Map<Integer, String> listNameComment = new HashMap<>();

        if (loggedInAccount == null) {
            return "redirect:/login";
        }
        model.addAttribute("loggedInAccount", loggedInAccount);


        Optional<Asset> optionalAsset = assetService.findAssetById(id);
        Type oldType = typeService.getTypeByAssetId(id);

        Status oldStatus = statusService.getStatusByAssetId(id);
        Optional<Warehouse> warehouseOptional = warehouseService.getQuantityByAssetId(id);
        if (warehouseOptional.isPresent()) {
            Warehouse warehouse = warehouseOptional.get();
            model.addAttribute("warehouse", warehouse);
        }

        model.addAttribute("oldType", oldType);

        if (optionalAsset.isPresent()) {
            Asset asset = optionalAsset.get();
            model.addAttribute("asset", asset);

            List<Comment> comments = commentService.findCommentByAssetId(id);
            for (Comment c : comments) {
                String accountFullName = commentService.findAccountNameById(c.getId());
                listNameComment.put(c.getId(), accountFullName);
            }
            model.addAttribute("listNameComment", listNameComment);
            model.addAttribute("comments", comments);
            model.addAttribute("newComment", new Comment());


            Optional<Status> statusOptional = assetService.getStatusByAssetId(asset.getIdAsset());
            if (statusOptional.isPresent()) {
                Status status = statusOptional.get();
                statusMap.put(asset.getIdAsset(), status);
            }

            Optional<Type> typeOptional = assetService.getTypeByAssetId(asset.getIdAsset());
            if (typeOptional.isPresent()) {
                Type type = typeOptional.get();
                typeMap.put(asset.getIdAsset(), type);
            }
        } else {
            model.addAttribute("errorMessage", "Not found asset with ID: " + id);
        }

        model.addAttribute("statusMap", statusMap);
        model.addAttribute("typeMap", typeMap);
        model.addAttribute("oldStatus", oldStatus);
        return "detail-asset";
    }

    @PostMapping("/asset/{id}/comment")
    public String addComment(@PathVariable("id") int id, @ModelAttribute("comment") Comment newComment) {
        int acId = (int) (newComment.getAccountId());
        newComment.setAccountId(acId);
        newComment.setContent(newComment.getContent());
        newComment.setAssetId(id);
        newComment.setCreatedAt(new Date());
        commentService.saveComment(newComment);
        return "redirect:/detailAsset/" + id;
    }

    @PostMapping("updateAsset/{id}")
    public String updateAsset(@PathVariable("id") int id, @ModelAttribute("asset") Asset asset, @RequestParam("newStockQuantity") int newQuantity, @RequestParam("newUnavailable") int unavalQuantity) throws Exception {
        assetService.updateAsset(id, asset);
        warehouseService.updateQuantity(id, newQuantity, unavalQuantity);
        return "redirect:/home";
    }


    @PostMapping("deleteAsset/{id}")
    public String deleteAsset(@PathVariable("id") String id) throws Exception {
        Optional<Asset> optionalAsset = assetService.findAssetById(Integer.parseInt(id));
        if (optionalAsset.isPresent()) {
            Asset asset = optionalAsset.get();
            assetService.deleteAsset(Integer.parseInt(id));
        }

        return "redirect:/home";
    }

    @PostMapping("/preview")
    public ResponseEntity<List<List<String>>> previewFile(@RequestParam("file") MultipartFile file) {
        List<List<String>> previewData = assetService.previewFile(file);
        return ResponseEntity.ok(previewData);
    }

    @PostMapping("/upload")
    public ResponseEntity<List<Asset>> uploadFile(@RequestParam("file") MultipartFile file) {
        List<Asset> assets = assetService.saveFile(file);
        return ResponseEntity.ok(assets);
    }




}