
import 'package:equatable/equatable.dart';

/// Represents a paginated list of items.
class PaginatedList<T> extends Equatable {
  final List<T> items;
  final int page;
  final int totalPages;
  final int totalItems;
  final bool hasMore;
  final bool isLoading;
  final bool isLoadingMore;

  const PaginatedList({
    required this.items,
    required this.page,
    required this.totalPages,
    required this.totalItems,
    required this.hasMore,
    this.isLoading = false,
    this.isLoadingMore = false
  });

  const PaginatedList.initial() : items = const [],
  page = 0,
  totalPages = 0,
  totalItems = 0,
  hasMore = true,
  isLoading = false,
  isLoadingMore = false;

  PaginatedList<T> copyWith({
    List<T>? items,
    int? page,
    int? totalPages,
    int? totalItems,
    bool? hasMore,
    bool? isLoading,
    bool? isLoadingMore
}) => PaginatedList(
    items: items ?? this.items,
    page: page ?? this.page,
    totalPages: totalPages ?? this.totalPages,
    totalItems: totalItems ?? this.totalItems,
    hasMore: hasMore ?? this.hasMore,
    isLoading: isLoading ?? this.isLoading,
    isLoadingMore: isLoadingMore ?? this.isLoadingMore
  );

  PaginatedList<T> appendPage({
    required List<T> newItems,
    required int nextPage,
    required int totalPages,
    required int totalItems,
  }) {
    return PaginatedList<T>(
      items: [...items, ...newItems],
      page: nextPage,
      totalPages: totalPages,
      totalItems: totalItems,
      hasMore: nextPage < totalPages,
      isLoading: false,
      isLoadingMore: false,
    );
  }

  PaginatedList<T> reset() => const PaginatedList.initial() as PaginatedList<T>;

  bool get isEmpty => items.isEmpty;
  bool get isNotEmpty => items.isNotEmpty;
  int get nextPage => page + 1;

  @override
  List<Object?> get props => [
    items,
    page,
    totalPages,
    totalItems,
    hasMore,
    isLoading,
    isLoadingMore
  ];
}

/// API response for paginated lists
class PagedResponse<T> {
  final List<T> data;
  final PaginationMeta meta;

  const PagedResponse({
    required this.data,
    required this.meta,
  });

  factory PagedResponse.fromJson(
      Map<String, dynamic> json,
      T Function(Map<String, dynamic>) fromJson,
      ) {
    return PagedResponse(
      data: (json['data'] as List).map((e) => fromJson(e as Map<String, dynamic>)).toList(),
      meta: PaginationMeta.fromJson(json['meta'] as Map<String, dynamic>),
    );
  }
}



class PaginationMeta {
  final int currentPage;
  final int lastPage;
  final int perPage;
  final int total;

  const PaginationMeta({
    required this.currentPage,
    required this.lastPage,
    required this.perPage,
    required this.total,
  });

  factory PaginationMeta.fromJson(Map<String, dynamic> json) {
    return PaginationMeta(
      currentPage: json['current_page'] as int,
      lastPage: json['last_page'] as int,
      perPage: json['per_page'] as int,
      total: json['total'] as int,
    );
  }
}
