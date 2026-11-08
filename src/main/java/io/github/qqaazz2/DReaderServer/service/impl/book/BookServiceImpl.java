package io.github.qqaazz2.DReaderServer.service.impl.book;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.qqaazz2.DReaderServer.common.BizException;
import io.github.qqaazz2.DReaderServer.dto.files.SyncProgressItem;
import io.github.qqaazz2.DReaderServer.entity.book.Book;
import io.github.qqaazz2.DReaderServer.entity.files.Files;
import io.github.qqaazz2.DReaderServer.entity.files.FilesDetails;
import io.github.qqaazz2.DReaderServer.mapper.book.BookMapper;
//import io.github.qqaazz2.DReaderServer.mapper.book.SeriesMapper;
import io.github.qqaazz2.DReaderServer.mapper.files.FilesDetailsMapper;
import io.github.qqaazz2.DReaderServer.mapper.files.FilesMapper;
import io.github.qqaazz2.DReaderServer.service.book.BookService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {
    //    @Resource
//    BookMapper bookMapper;
//
//    @Resource
//    SeriesService seriesService;
//
//    @Resource
//    UploadService uploadService;
//
    @Resource
    FilesMapper filesMapper;
//
//
//    @Value("${file.upload}")
//    String filePath;
//

    @Resource
    FilesDetailsMapper filesDetailsMapper;

    @Override
    public List<Book> createData(List<Book> list) {
        Boolean isTrue = this.saveBatch(list);
        if (!isTrue) throw new BizException("4000", "新增书籍阅读进度失败");
        return list;
    }

    @Override
    public Book getData(Long filesId) {
        LambdaQueryWrapper<Book> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Book::getFilesId, filesId);
        Book book = this.getOne(lambdaQueryWrapper);
        if (book == null) return createData(filesId);
        return book;
    }

    @Override
    public Book createData(Long filesId) {
        Book book = new Book();
        book.setFilesId(filesId);
        if (!this.save(book)) throw new BizException("4000", "新增书籍阅读进度失败");
        return book;
    }

    //
//    @Override
//    public PageVO<BookListDTO> getList(BookListQueryCondition queryCondition) {
//        List<BookListDTO> list = bookMapper.getList(queryCondition);
//        list.stream().forEach(item -> item.setMinioCover(uploadService.getObject(item.getCover())));
//        System.out.println(list);
//        Integer count = bookMapper.count(queryCondition);
//        return new PageVO(queryCondition.getLimit(), queryCondition.getPage(), count, list);
//    }
//
//    @Override
//    public Book updateData(Book book) {
//        Boolean isTrue = this.updateById(book);
//        if (!isTrue) throw new BizException("4000", "编辑书籍信息失败");
//        return book;
//    }
//
    @Override
    public Map<String, Object> updateProgress(Book book) {
        LambdaUpdateWrapper<Book> updateWrapper = new LambdaUpdateWrapper();
        updateWrapper.eq(Book::getId, book.getId()).set(Book::getProgress, book.getProgress()).set(Book::getReadTagNum, book.getReadTagNum());
        Boolean isTrue = this.update(updateWrapper);
        if (!isTrue) throw new BizException("4000", "阅读进度修改失败");
        int status = getStatus(book.getProgress());

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formatted = now.format(formatter);
        UpdateWrapper<FilesDetails> filesDetailsUpdateWrapper = new UpdateWrapper<>();
        filesDetailsUpdateWrapper.set("status", status);
        filesDetailsUpdateWrapper.eq("files_id", book.getFilesId());
        filesDetailsUpdateWrapper.set("last_read_time", formatted);
        if (filesDetailsMapper.update(filesDetailsUpdateWrapper) == 0) throw new BizException("4000", "阅读进度修改失败");

        Map<String, Object> map = new HashMap<>();
        map.put("lastReadTime", formatted);
        map.put("status", status);
        return map;
    }

    @Override
    @Transactional
    public void syncProgress(List<SyncProgressItem> list) {
        Map<String, SyncProgressItem> syncProgressItemMap = list.stream().collect(Collectors.toMap(SyncProgressItem::getHash, item -> item));
        Set<String> hashSet = syncProgressItemMap.keySet();
        LambdaQueryWrapper<Files> filesLambdaQueryWrapper = new LambdaQueryWrapper<>();
        filesLambdaQueryWrapper.in(Files::getHash, hashSet);
        List<Files> filesList = filesMapper.selectList(filesLambdaQueryWrapper);
        if (filesList.isEmpty()) throw new BizException("4000", "书籍查询失败，请稍后再试");

        LambdaQueryWrapper<Book> bookLambdaQueryWrapper = new LambdaQueryWrapper<>();
        bookLambdaQueryWrapper.in(Book::getFilesId, filesList.stream().map(item -> item.getId()).collect(Collectors.toList()));
        List<Book> bookList = this.list(bookLambdaQueryWrapper);
        Map<Long, Book> bookMap = bookList.stream().collect(Collectors.toMap(Book::getFilesId, item -> item));

        List<Book> addList = new ArrayList<>();
        for (Files files : filesList) {
            if (!hashSet.contains(files.getHash())) continue;
            Book book = bookMap.get(files.getId());
            SyncProgressItem item = syncProgressItemMap.get(files.getHash());
            if (book != null) {
                if(item.getProgress() == book.getProgress()) continue;
                LambdaUpdateWrapper<Book> updateWrapper = new LambdaUpdateWrapper();
                updateWrapper.eq(Book::getId, book.getId()).set(Book::getProgress, item.getProgress()).set(Book::getReadTagNum, item.getReadTagNum());
                if(!this.update(updateWrapper)) throw new BizException("4000", "阅读进度同步失败1，请稍后再试");
                UpdateWrapper<FilesDetails> filesDetailsUpdateWrapper = new UpdateWrapper<>();
                filesDetailsUpdateWrapper.set("status", getStatus(item.getProgress()));
                filesDetailsUpdateWrapper.eq("files_id", book.getFilesId());
                if(filesDetailsMapper.update(filesDetailsUpdateWrapper) == 0)throw new BizException("4000", "阅读进度同步失败2，请稍后再试");
                continue;
            }
            book.setProgress(item.getProgress());
            book.setReadTagNum(item.getReadTagNum());
            book.setFilesId(files.getId());
            addList.add(book);
            UpdateWrapper<FilesDetails> filesDetailsUpdateWrapper = new UpdateWrapper<>();
            filesDetailsUpdateWrapper.set("status", getStatus(item.getProgress()));
            filesDetailsUpdateWrapper.eq("files_id", book.getFilesId());
            if(filesDetailsMapper.update(filesDetailsUpdateWrapper) == 0)throw new BizException("4000", "阅读进度同步失败2，请稍后再试");
        }
        if(addList.isEmpty())return;
        if(!this.saveBatch(addList)) throw new BizException("4000", "阅读进度同步失败3，请稍后再试");
    }

    private Integer getStatus(Double progress){
        int status;
        if (progress == 0) {
            status = 1;
        } else if (progress > 0 && progress < 1) {
            status = 3;
        } else {
            status = 2;
        }
        return  status;
    }
//
//
//        if (status == 2) {
//            List<BookListDTO> list = bookMapper.getListByParentId(bookListDTO.getParentId());
//            Long count = list.stream().filter(value -> value.getStatus() != 2).count();
//            if (count == 0) {
//                seriesService.updateStatus(bookListDTO.getParentId(), 2);
//                map.put("status", 2);
//            }
//        } else {
//            seriesService.updateStatus(bookListDTO.getParentId(), 3);
//        }
//        return map;
//    }
//
//    @Override
//    public List<BookCoverDTO> getCoverList(Integer id) {
//        return bookMapper.getCoverList(id);
//    }
//
//    @Override
//    public BookListDTO getRecent() {
//        List<BookListDTO> list = bookMapper.getRecent();
//        if (bookMapper.getRecent().isEmpty()) {
//            return null;
//        }
//        BookListDTO bookListDTO = list.get(0);
//        bookListDTO.setMinioCover(uploadService.getObject(bookListDTO.getCover()));
//        return bookListDTO;
//    }
//
//    @Override
//    public Map<String, Integer> getOverview() {
//        HashMap<String, Integer> map = new HashMap<>();
//        Integer seriesCount = seriesMapper.count(new SeriesListQueryCondition(0));
//        BookListQueryCondition queryCondition = new BookListQueryCondition(0);
//        Integer bookCount = bookMapper.count(queryCondition);
//        queryCondition.setStatus(2);
//        Integer overCount = bookMapper.count(queryCondition);
//        queryCondition.setStatus(1);
//        Integer unreadCount = bookMapper.count(queryCondition);
//
//        map.put("seriesCount", seriesCount);
//        map.put("bookCount", bookCount);
//        map.put("overCount", overCount);
//        map.put("unreadCount", unreadCount);
//        map.put("readingCount", bookCount - overCount - unreadCount);
//
//        return map;
//    }
//
//    @Override
//    @Transactional
//    public Map<String, String> changeCover(Integer id, MultipartFile multipartFile) {
//        FileTypeUtils.validateFile(multipartFile, new String[]{"jpg"}, 10240);
//        Map<String, String> map = new HashMap<>();
//        BookFileCover bookCover = bookMapper.getBookCover(id);
//        if (bookCover == null) throw new BizException("4000", "找不到对应的书籍文件");
//        String path = "";
//        if (bookCover.getCover() == null || bookCover.getCover().isEmpty() || bookCover.getCover().isBlank()) {
//            path = filePath + "books/" + bookCover.getParentFileName() + "/" + bookCover.getHash() + ".jpg";
//            LambdaUpdateWrapper<Book> lambdaUpdateWrapper = new LambdaUpdateWrapper();
//            lambdaUpdateWrapper.set(Book::getCover, path).eq(Book::getId, id);
//            boolean isTrue = this.update(lambdaUpdateWrapper);
//            if (!isTrue) throw new BizException("4000", "修改" + bookCover.getName() + "信息失败");
//        } else path = bookCover.getCover();
//        try {
//            String minioUrl = uploadService.upload(multipartFile.getBytes(), path, "image/jpeg");
//            map.put("minioUrl", uploadService.getObject(minioUrl));
//            map.put("cover", path);
//            return map;
//        } catch (IOException e) {
//            e.printStackTrace();
//            throw new BizException("4000", "修改" + bookCover.getName() + "封面图片失败");
//        }
//    }
}
